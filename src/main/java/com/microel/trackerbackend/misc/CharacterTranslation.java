package com.microel.trackerbackend.misc;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CharacterTranslation {
    private final static Map<Character, Character> characterMap = Stream.of(new Character[][]{
            {'q', 'й'},
            {'w', 'ц'},
            {'e', 'у'},
            {'r', 'к'},
            {'t', 'е'},
            {'y', 'н'},
            {'u', 'г'},
            {'i', 'ш'},
            {'o', 'щ'},
            {'p', 'з'},
            {'[', 'х'},
            {']', 'ъ'},
            {'a', 'ф'},
            {'s', 'ы'},
            {'d', 'в'},
            {'f', 'а'},
            {'g', 'п'},
            {'h', 'р'},
            {'j', 'о'},
            {'k', 'л'},
            {'l', 'д'},
            {';', 'ж'},
            {'\'', 'э'},
            {'z', 'я'},
            {'x', 'ч'},
            {'c', 'с'},
            {'v', 'м'},
            {'b', 'и'},
            {'n', 'т'},
            {'m', 'ь'},
            {',', 'б'},
            {'.', 'ю'},
            {'Q', 'Й'},
            {'W', 'Ц'},
            {'E', 'У'},
            {'R', 'К'},
            {'T', 'Е'},
            {'Y', 'Н'},
            {'U', 'Г'},
            {'I', 'Ш'},
            {'O', 'Щ'},
            {'P', 'З'},
            {'{', 'Х'},
            {'}', 'Ъ'},
            {'A', 'Ф'},
            {'S', 'Ы'},
            {'D', 'В'},
            {'F', 'А'},
            {'G', 'П'},
            {'H', 'Р'},
            {'J', 'О'},
            {'K', 'Л'},
            {'L', 'Д'},
            {':', 'Ж'},
            {'"', 'Э'},
            {'Z', 'Я'},
            {'X', 'Ч'},
            {'C', 'С'},
            {'V', 'М'},
            {'B', 'И'},
            {'N', 'Т'},
            {'M', 'Ь'},
            {'<', 'Б'},
            {'>', 'Ю'}
    }).collect(Collectors.toMap(data -> data[0], data -> data[1]));

    public static String translate(String input) {
        StringBuilder result = new StringBuilder();
        char[] chars = input.toCharArray();
        int index = 0;
        for(Character character : chars) {
            if(((index > 0 && characterMap.containsKey(chars[index-1])) || (index < chars.length-1 && characterMap.containsKey(chars[index+1]))) && characterMap.containsKey(character)){
                result.append(characterMap.getOrDefault(character, character));
                continue;
            }
            result.append(character);
            index++;
        }
        return result.toString();
    }
}
