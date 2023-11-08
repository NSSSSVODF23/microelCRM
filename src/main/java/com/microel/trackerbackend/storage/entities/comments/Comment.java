package com.microel.trackerbackend.storage.entities.comments;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.microel.trackerbackend.storage.entities.task.Task;
import com.microel.trackerbackend.storage.entities.team.Employee;
import lombok.*;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "comments")
public class Comment implements TaskJournalItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long commentId;
    @Column(columnDefinition = "text default ''")
    private String message;
    private Timestamp created;
    @ManyToOne
    @OnDelete(action = OnDeleteAction.NO_ACTION)
    @JoinColumn(name = "f_employee_id")
    private Employee creator;
    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    @BatchSize(size = 25)
    private List<Attachment> attachments;

    @ManyToOne
    @OnDelete(action = OnDeleteAction.NO_ACTION)
    @JoinColumn(name = "f_reply_comment_id")
    private Comment replyComment;
    private Boolean edited;
    private Boolean deleted;
    @ManyToOne(cascade = {CascadeType.MERGE})
    @OnDelete(action = OnDeleteAction.NO_ACTION)
    @JsonBackReference
    private Task parent;

    /**
     * Отчищает сообщение от форматирования и оставляет просто текст
     * @return Сообщение без форматирования
     */
    public String getSimpleText() {
        if(message == null) return "";
        return message.replaceAll("</p>", "\n").replaceAll("<.*?>", "");
    }

    /**
     * Парсит текст сообщения и получает всех сотрудников упомянутых в нем
     * @return Список сотрудников
     */
    @JsonIgnore
    public List<String> getReferredLogins() {
        // Получить из сообщения все вхождения @employee
        List<String> logins = new ArrayList<>();
        Pattern pattern = Pattern.compile("@([^ ]+)");
        Matcher matcher = pattern.matcher(message);
        while (matcher.find()) {
            logins.add(matcher.group(1));
        }
        return logins;
    }

//    public void setAttachments(List<Attachment> attachments) {
//        this.attachments = attachments;
//    }
}
