package com.microel.trackerbackend.services;

import com.microel.trackerbackend.services.api.ResponseException;
import com.microel.trackerbackend.services.api.StompController;
import com.microel.trackerbackend.storage.entities.filesys.Directory;
import com.microel.trackerbackend.storage.entities.filesys.FileSystemItem;
import com.microel.trackerbackend.storage.entities.filesys.TFile;
import com.microel.trackerbackend.storage.exceptions.EntryNotFound;
import com.microel.trackerbackend.storage.repositories.DirectoryRepository;
import com.microel.trackerbackend.storage.repositories.FileRepository;
import com.microel.trackerbackend.storage.repositories.FileSystemItemRepository;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.Sort;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Transactional(readOnly = true)
public class FilesWatchService {
    private final FileSystemItemRepository fileSystemItemRepository;
    private final FileRepository fileRepository;
    private final DirectoryRepository directoryRepository;
    private final StompController stompController;
    Path rootPath = null;

    public FilesWatchService(FileSystemItemRepository fileSystemItemRepository, FileRepository fileRepository, DirectoryRepository directoryRepository, StompController stompController) throws IOException {
        this.fileSystemItemRepository = fileSystemItemRepository;
        this.fileRepository = fileRepository;
        this.directoryRepository = directoryRepository;
        this.stompController = stompController;
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            if (rootPath == null) {
                try {
                    rootPath = Paths.get("./files");
                    boolean exists = Files.exists(rootPath);
                    if (!exists) {
                        boolean mkdir = rootPath.toFile().mkdir();
                        if (!mkdir) {
                            System.out.println("Не удалось начать отслеживание файлов, отсутствует директория ./files");
                            return;
                        }
                    }
                } catch (InvalidPathException e) {
                    boolean exists = Files.exists(rootPath);
                    if (!exists) {
                        boolean mkdir = rootPath.toFile().mkdir();
                        if (!mkdir) {
                            System.out.println("Не удалось начать отслеживание файлов, отсутствует директория ./files");
                            return;
                        }
                    }
                }
                syncAll();
            }
        });
        executor.shutdown();
    }

    @Nullable
    public Directory getTargetDirectoryEntity(@Nullable Long targetDirectory){
        return targetDirectory != null ? directoryRepository.findById(targetDirectory).orElseThrow(() -> new EntryNotFound("Целевая директория не найдена")) : null;
    }

    public Path getTargetDirectoryPath(Directory targetDirectoryEntity){
        return targetDirectoryEntity != null ? Path.of(targetDirectoryEntity.getPath()) : rootPath;
    }

    public List<FileSystemItem> getSourceFileItems(Set<Long> sourceFiles){
        if(sourceFiles == null || sourceFiles.isEmpty())
            throw new ResponseException("Не указаны файлы");
        return fileSystemItemRepository.findAll((root, query, cb)-> root.get("fileSystemItemId").in(sourceFiles));
    }

    public Map<Boolean, List<FileSystemItem>> checkFilesExist(List<FileSystemItem> sourceFiles){
        return sourceFiles.stream().collect(Collectors.partitioningBy(f->{
            Path entryPath = Path.of(f.getPath());
            return Files.exists(entryPath);
        }));
    }

    public void sendUpdateDirectorySignal(@Nullable Directory directory){
        stompController.updateFilesDirectory(directory != null ? directory.getFileSystemItemId() : 0);
    }

    @Transactional
    public void moveFiles(Set<Long> movingFiles, @Nullable Long targetDirectory){

        Set<Long> directoriesUpdated = new HashSet<>();
        directoriesUpdated.add(targetDirectory != null ? targetDirectory : 0L);

        Directory targetDirectoryEntity = getTargetDirectoryEntity(targetDirectory);
        Path targetDirectoryPath = getTargetDirectoryPath(targetDirectoryEntity);
        if(!Files.exists(targetDirectoryPath)) throw new ResponseException("Целевая директория не существует");

        List<FileSystemItem> movingFilesEntities = getSourceFileItems(movingFiles);
        Map<Boolean, List<FileSystemItem>> existingCheckedMap = checkFilesExist(movingFilesEntities);
        throwIsMoveInSelf(targetDirectoryEntity, movingFilesEntities);

        fileSystemItemRepository.deleteAll(existingCheckedMap.get(false));
        for (FileSystemItem mf : existingCheckedMap.get(true)) {
            if(Objects.equals(mf.getParent(), targetDirectoryEntity)) continue;
            Path movingEntryPath = Path.of(mf.getPath());
            String resolvePath = targetDirectoryPath.resolve(movingEntryPath.getFileName()).toString();
            while(existsByPath(resolvePath)){
                resolvePath = unCollidingPath(resolvePath);
            }
            try {
                Path newPath = Path.of(resolvePath);
                Files.move(movingEntryPath, newPath, StandardCopyOption.REPLACE_EXISTING);
                directoriesUpdated.add((mf.getParent() != null ? mf.getParent().getFileSystemItemId() : 0));
                if(mf instanceof Directory) {
                    updatePathInDirectory(mf.getFileSystemItemId(), newPath);
                }
                mf.setName(newPath.getFileName().toString());
                mf.setPath(newPath.toString());
                mf.setParent(targetDirectoryEntity);
                fileSystemItemRepository.save(mf);
            } catch (IOException e) {
                System.out.println("Не удалось переместить файл " + movingEntryPath);
            }
        }
        directoriesUpdated.forEach(stompController::updateFilesDirectory);
    }

    private void throwIsMoveInSelf(Directory targetDirectoryEntity, List<FileSystemItem> movingFilesEntities) {
        Directory parent = targetDirectoryEntity;
        while (parent != null){
            for (FileSystemItem mf : movingFilesEntities) {
                if(mf instanceof Directory)
                    if(Objects.equals(mf, parent))
                        throw new ResponseException("Нельзя переместить директорию саму в себя");
            }
            parent = parent.getParent();
        }
    }

    @Transactional
    public void copyFiles(Set<Long> copyingFiles, @Nullable Long targetDirectory){

        Set<Long> directoriesUpdated = new HashSet<>();
        directoriesUpdated.add(targetDirectory != null ? targetDirectory : 0L);

        Directory targetDirectoryEntity = getTargetDirectoryEntity(targetDirectory);
        Path targetDirectoryPath = getTargetDirectoryPath(targetDirectoryEntity);
        if(!Files.exists(targetDirectoryPath)) throw new ResponseException("Целевая директория не существует");

        List<FileSystemItem> copiedFilesEntities = getSourceFileItems(copyingFiles);
        Map<Boolean, List<FileSystemItem>> existingCheckedMap = checkFilesExist(copiedFilesEntities);

        throwIsMoveInSelf(targetDirectoryEntity, copiedFilesEntities);

        fileSystemItemRepository.deleteAll(existingCheckedMap.get(false));
        for (FileSystemItem mf : existingCheckedMap.get(true)) {
            if(Objects.equals(mf.getParent(), targetDirectoryEntity)) continue;
            Path copiedEntryPath = Path.of(mf.getPath());
            String resolvePath = targetDirectoryPath.resolve(copiedEntryPath.getFileName()).toString();
            while(existsByPath(resolvePath)){
                resolvePath = unCollidingPath(resolvePath);
            }
            try {
                Path newPath = Path.of(resolvePath);
                Files.copy(copiedEntryPath, Path.of(resolvePath), StandardCopyOption.REPLACE_EXISTING);
                if(mf instanceof Directory) {
                    updatePathInDirectory(mf.getFileSystemItemId(), newPath);
                }
                FileSystemItem fileSystemItemCopy = mf.copy(targetDirectoryEntity, newPath.toString());
                fileSystemItemCopy.setName(newPath.getFileName().toString());
                fileSystemItemRepository.save(fileSystemItemCopy);
            } catch (IOException e) {
                System.out.println("Не удалось скопировать файл " + copiedEntryPath);
            }
        }
        directoriesUpdated.forEach(stompController::updateFilesDirectory);
    }

    @Transactional
    public void updatePathInDirectory(Long targetDirectoryId, Path newPath){
        List<FileSystemItem> filesInDirectory = fileSystemItemRepository.findAll((root, query, cb)->{
            Join<FileSystemItem, Directory> parentJoin = root.join("parent",  JoinType.LEFT);
            return cb.equal(parentJoin.get("fileSystemItemId"), targetDirectoryId);
        });
        for (FileSystemItem fileInDirectory : filesInDirectory) {
            Path fileInDirectoryPath = Path.of(fileInDirectory.getPath());
            Path resolvePathInDirectory = newPath.resolve(fileInDirectoryPath.getFileName());
            fileInDirectory.setPath(resolvePathInDirectory.toString());
            fileSystemItemRepository.save(fileInDirectory);
            if(fileInDirectory instanceof Directory){
                updatePathInDirectory(fileInDirectory.getFileSystemItemId(), resolvePathInDirectory);
            }
        }
    }

    public String unCollidingPath(String resolvePath){
        int i = resolvePath.lastIndexOf('.');
        StringBuilder sb = new StringBuilder(resolvePath);
        sb.insert(i, " new");
        return sb.toString();
    }

    @Transactional
    public void deleteFile(Long id){
        FileSystemItem fileSystemItem = fileSystemItemRepository.findById(id).orElseThrow(() -> new EntryNotFound("Файл не найден"));
        try {
            Files.deleteIfExists(Path.of(fileSystemItem.getPath()));
            fileSystemItemRepository.delete(fileSystemItem);
            sendUpdateDirectorySignal(fileSystemItem.getParent());
        } catch (IOException e) {
            throw new ResponseException("Не удалось удалить файл");
        }
    }

    @Transactional
    public void renameFile(Long id, String newName){
        if(newName.isBlank()) throw new ResponseException("Неверное имя файла");
        FileSystemItem foundFile = fileSystemItemRepository.findById(id).orElseThrow(() -> new EntryNotFound("Файл не найден"));
        Path newPath = Path.of(foundFile.getPath()).resolveSibling(newName);
        if(existsByPath(newPath.toString())){
            throw new ResponseException("Файл с таким именем уже существует");
        }
        try {
            Files.move(Path.of(foundFile.getPath()), newPath, StandardCopyOption.REPLACE_EXISTING);
            foundFile.setName(newName);
            foundFile.setPath(newPath.toString());
            if(foundFile instanceof Directory) {
                updatePathInDirectory(foundFile.getFileSystemItemId(), newPath);
            }else if(foundFile instanceof TFile){
                ((TFile) foundFile).setMimeType(Files.probeContentType(newPath));
            }
            fileSystemItemRepository.save(foundFile);
            sendUpdateDirectorySignal(foundFile.getParent());
        } catch (IOException e) {
            throw new ResponseException("Не удалось переименовать файл");
        }
    }

    public boolean existsByPath(String path){
        return Files.exists(Path.of(path)) || !fileSystemItemRepository.findAll((root, query, cb)-> cb.equal(root.get("path"), path)).isEmpty();
    }

    @Transactional
    @Async
    @Scheduled(cron="0 0 */1 * * *")
    public void syncAll(){
        clearFiles();
        try (Stream<Path> paths = Files.walk(rootPath)) {
            paths.filter(p -> !p.equals(rootPath)).forEach(this::syncPath);
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    @Transactional
    public void syncPath(Path path) {
        File file = path.toFile();
        if (file.isDirectory()) {
            Directory directory = Directory.of(file);
            Directory existDirectory = directoryRepository.findFirstByPath(directory.getPath()).orElse(null);
            if (existDirectory != null) {
                update(directory, existDirectory);
                sendUpdateDirectorySignal(directory.getParent());
            } else {
                if (file.getParentFile() != null
                        && file.getParentFile().isDirectory()
                        && !Objects.equals(file.getParentFile().toPath(), rootPath)) {
                    Directory parentDirectory = createAndGetParentDirectory(file.getParentFile());
                    directory.setParent(parentDirectory);
                }
                create(directory);
                sendUpdateDirectorySignal(directory.getParent());
            }
        } else {
            TFile tFile = TFile.of(file);
            TFile existTFile = fileRepository.findFirstByPath(tFile.getPath()).orElse(null);
            if (existTFile != null) {
                update(tFile, existTFile);
                sendUpdateDirectorySignal(tFile.getParent());
            } else {
                if (file.getParentFile() != null
                        && file.getParentFile().isDirectory()
                        && !Objects.equals(file.getParentFile().toPath(), rootPath)) {
                    Directory parentDirectory = createAndGetParentDirectory(file.getParentFile());
                    tFile.setParent(parentDirectory);
                }
                create(tFile);
                sendUpdateDirectorySignal(tFile.getParent());
            }
        }
    }

    @Transactional
    @Nullable
    public Directory createAndGetParentDirectory(File file) {
        Directory directory = Directory.of(file);
        Directory parentDirectory = null;
        File parentFile = file.getParentFile();
        if (parentFile != null && parentFile.isDirectory() && !Objects.equals(parentFile, rootPath.toFile())) {
            parentDirectory = createAndGetParentDirectory(parentFile);
        }
        directory.setParent(parentDirectory);
        return directoryRepository.findFirstByPath(directory.getPath()).orElseGet(() -> directoryRepository.save(directory));
    }

    @Transactional
    @Nullable
    public Directory create(Directory directory) {
        Directory existDirectory = directoryRepository.findFirstByPath(directory.getPath()).orElse(null);
        if (existDirectory == null) {
            return directoryRepository.save(directory);
        }
        return null;
    }

    @Transactional
    public Directory update(Directory directory, Directory existDirectory) {
        existDirectory.setFileCount(directory.getFileCount());
        existDirectory.setCreatedAt(directory.getCreatedAt());
        existDirectory.setModifiedAt(directory.getModifiedAt());
        return directoryRepository.save(existDirectory);
    }

    @Transactional
    @Nullable
    public TFile create(TFile TFile) {
        TFile existTFile = fileRepository.findFirstByPath(TFile.getPath()).orElse(null);
        if (existTFile == null) {
            return fileRepository.save(TFile);
        }
        return null;
    }

    @Transactional
    public TFile update(TFile tFile, TFile existTFile) {
        existTFile.setSize(tFile.getSize());
        existTFile.setMimeType(tFile.getMimeType());
        existTFile.setCreatedAt(tFile.getCreatedAt());
        existTFile.setModifiedAt(tFile.getModifiedAt());
        return fileRepository.save(existTFile);
    }

    @Transactional
    public void clearFiles(){
        List<FileSystemItem> idToDelete = new ArrayList<>();
        fileSystemItemRepository.findAll().forEach(fileSystemItem -> {
            Path path = Path.of(fileSystemItem.getPath());
            if(!Files.exists(path)){
                idToDelete.add(fileSystemItem);
            }
        });
        fileSystemItemRepository.deleteAll(idToDelete);
    }

    public List<FileSystemItem> getRoot(@Nullable FileSortingTypes sortingType){
        return fileSystemItemRepository.findAll((root, query, cb)-> cb.isNull(root.get("parent")), getSortBy(sortingType));
    }

    public List<Directory> getPath(Long id){
        List<Directory> path = new ArrayList<>();
        FileSystemItem targetDirectory = fileSystemItemRepository.findById(id).orElseThrow(()->new ResponseException("Ошибка при построении пути"));
        Directory parentDirectory = targetDirectory.getParent();
        while (parentDirectory != null){
            path.add(parentDirectory);
            parentDirectory = parentDirectory.getParent();
        }
        Collections.reverse(path);
        return path;
    }

    public LoadingDirectoryWrapper getDirectory(Long id, @Nullable FileSortingTypes sortingType){
        LoadingDirectoryWrapper wrapper = new LoadingDirectoryWrapper();
        wrapper.setFiles(fileSystemItemRepository.findAll((root, query, cb)->{
            Join<FileSystemItem, Directory> parentJoin = root.join("parent",  JoinType.LEFT);
            return cb.equal(parentJoin.get("fileSystemItemId"), id);
        }, getSortBy(sortingType)));
        wrapper.setPath(getPath(id));
        return wrapper;
    }

    private Sort getSortBy(@Nullable FileSortingTypes sortingType){
        if(sortingType == null) sortingType = FileSortingTypes.NAME_ASC;
        Sort sort = Sort.by( Sort.Direction.ASC,"discriminator");
        switch (sortingType) {
            case NAME_ASC -> sort = sort.and(Sort.by(Sort.Direction.ASC, "name"));
            case NAME_DESC -> sort = sort.and(Sort.by(Sort.Direction.DESC, "name"));
            case SIZE_ASC -> sort = sort.and(Sort.by(Sort.Direction.ASC, "size"));
            case SIZE_DESC -> sort = sort.and(Sort.by(Sort.Direction.DESC, "size"));
            case CREATE_DATE_ASC -> sort = sort.and(Sort.by(Sort.Direction.ASC,"createdAt"));
            case CREATE_DATE_DESC -> sort = sort.and(Sort.by(Sort.Direction.DESC,"createdAt"));
            case MODIFY_DATE_ASC -> sort = sort.and(Sort.by(Sort.Direction.ASC,"modifiedAt"));
            case MODIFY_DATE_DESC -> sort = sort.and(Sort.by(Sort.Direction.DESC,"modifiedAt"));
        }
        return sort;
    }

    @Transactional
    public void loadFile(String fileName, byte[] fileData, @Nullable Long targetDirectoryId){
        Directory directory = targetDirectoryId == null ? null : directoryRepository.findById(targetDirectoryId).orElseThrow(()->new ResponseException("Целевая директория не найдена"));
        Path targetPath = directory == null ? rootPath : Path.of(directory.getPath());
        if(!Files.exists(targetPath)){
            throw new ResponseException("Целевая директория не существует");
        }
        String filePath = targetPath.resolve(fileName).toString();
        while(existsByPath(filePath)){
            filePath = unCollidingPath(filePath);
        }
        try {
            Files.write(Path.of(filePath), fileData);
            syncPath(Path.of(filePath));
        }catch (IOException e){
            throw new ResponseException("Не удалось загрузить файл");
        }
    }

    @Transactional
    public void createDirectory(@Nullable Long parentDirectoryId, String name) {
        Directory parentDirectory = parentDirectoryId == null ? null : directoryRepository.findById(parentDirectoryId).orElseThrow(()->new ResponseException("Родительская директория не найдена"));
        Path parentPath = parentDirectory == null ? rootPath : Path.of(parentDirectory.getPath());
        if(!Files.exists(parentPath)){
            throw new ResponseException("Родительская директория не существует");
        }
        String directoryBeingCreated = parentPath.resolve(name).toString();
        while(existsByPath(directoryBeingCreated)){
            directoryBeingCreated = unCollidingPath(directoryBeingCreated);
        }
        try {
            Files.createDirectory(Path.of(directoryBeingCreated));
            syncPath(Path.of(directoryBeingCreated));
        }catch (IOException e){
            throw new ResponseException("Не удалось создать директорию");
        }
    }

    public List<FileSystemItem> search(String stringQuery, @Nullable FileSortingTypes sortingType){
        return fileSystemItemRepository.findAll((root, query, cb)-> cb.like(cb.lower(root.get("name")), "%" + stringQuery.toLowerCase() + "%"), getSortBy(sortingType));
    }

    public List<TFile> searchFiles(String stringQuery, @Nullable FileSortingTypes sortingType){
        return fileRepository.findAll((root, query, cb)-> cb.like(cb.lower(root.get("name")), "%" + stringQuery.toLowerCase() + "%"), getSortBy(sortingType));
    }

    public TFile getFileById(Long id) {
        return fileRepository.findById(id).orElseThrow(()->new ResponseException("Файл не найден"));
    }

    public enum FileSortingTypes{
        NAME_ASC("NAME_ASC"),
        NAME_DESC("NAME_DESC"),
        SIZE_ASC("SIZE_ASC"),
        SIZE_DESC("SIZE_DESC"),
        CREATE_DATE_ASC("CREATE_DATE_ASC"),
        CREATE_DATE_DESC("CREATE_DATE_DESC"),
        MODIFY_DATE_ASC("MODIFY_DATE_ASC"),
        MODIFY_DATE_DESC("MODIFY_DATE_DESC");


        private final String value;

        FileSortingTypes(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public String getLabel() {
            return switch (this) {
                case NAME_ASC -> "Имя (А-Я)";
                case NAME_DESC -> "Имя (Я-А)";
                case SIZE_ASC -> "Сначала маленькие";
                case SIZE_DESC -> "Сначала большие";
                case CREATE_DATE_ASC -> "Сначала старые";
                case CREATE_DATE_DESC -> "Сначала новые";
                case MODIFY_DATE_ASC -> "Давно измененные";
                case MODIFY_DATE_DESC -> "Недавно измененные";
            };
        }

        public static List<Map<String,String>> getList(){
            return Stream.of(FileSortingTypes.values()).map(value->Map.of("label", value.getLabel(), "value", value.getValue())).toList();
        }
    }

    @Getter
    @Setter
    public static class TransferEvent {
        @Nullable
        private Long target;
        private Set<Long> source;
    }

    @Getter
    @Setter
    public static class RenameEvent {
        private Long id;
        private String name;
    }

    @Getter
    @Setter
    public static class LoadFileEvent{
        private String name;
        private byte[] data;
        @Nullable
        private Long targetDirectoryId;
    }

    @Getter
    @Setter
    public static class CreateDirectoryEvent {
        @Nullable
        private Long parentDirectoryId;
        private String name;
    }

    @Getter
    @Setter
    public static class LoadingDirectoryWrapper {
        private List<Directory> path;
        private List<FileSystemItem> files;
    }
}
