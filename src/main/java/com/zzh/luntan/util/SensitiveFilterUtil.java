package com.zzh.luntan.util;

import jakarta.annotation.PostConstruct;
import org.apache.commons.lang3.CharUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;

@Component
public class SensitiveFilterUtil {
    private static final Logger logger = LoggerFactory.getLogger(SensitiveFilterUtil.class);
    // 根节点
    TreeNode root = new TreeNode();
    // 敏感词替换
    public static String SENSITIVEREPLACE = "***";

    // 内部类，树
    private class TreeNode {
        // 子节点
        private HashMap<Character, TreeNode> map = new HashMap<>();
        // 此节点是否是敏感词最后一个字符
        private boolean isEnd = false;

        public boolean isEnd() {
            return isEnd;
        }

        public void setEnd(boolean end) {
            isEnd = end;
        }

        public void setNode(Character c,TreeNode childNode) {
            map.put(c, childNode);
        }

        public TreeNode getChildNode(Character c) {
            return map.get(c);
        }
    }

    // 服务器启动时让Spring实例化管理此类对象调用构造器后调用此方法装载所有敏感词
    @PostConstruct
    public void init() {

        try (
                InputStream is = this.getClass().getClassLoader().getResourceAsStream("SensitiveWords.txt");
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        ) {
            String word;
            while ((word = reader.readLine()) != null) {
                // 某个敏感词添加到树中
                addWordToTree(word);
            }
        } catch (IOException e) {
            logger.error("加载敏感词失败" + e.getMessage());
        }
    }

    // 某个敏感词添加到树中
    public void addWordToTree(String word) {
        if (StringUtils.isBlank(word)) {
            return;
        }
        TreeNode template = root;
        for (int i = 0; i < word.length(); i++) {
            Character c = word.charAt(i);
            TreeNode childNode = template.getChildNode(c);
            if (childNode == null) {
                childNode= new TreeNode();
                template.setNode(c,childNode);
            }
            template = childNode;

        }
        // 设置结束标识
        template.setEnd(true);
    }

    // 语句过滤敏感词
    public String filter(String word) {
        if (StringUtils.isBlank(word)) {
            return word;
        }
        TreeNode template = root;
        int begin = 0;
        int position = 0;
        StringBuilder stringBuilder = new StringBuilder();
        while (begin < word.length()) {
            if (position < word.length()) {
                char c = word.charAt(position);
                // 如果是特殊符号跳过
                if (isSymbol(c)) {
                    // 如果此时指向根节点，说明是新的敏感词判断，
                    if (template == root) {
                        stringBuilder.append(c);
                        begin++;
                    }
                    position++;
                    continue;
                }

                // 指向下级节点
                template = template.getChildNode(c);

                if (template == null) {
                    // 说明begin字符不是敏感词开头，直接追加,template归位，下次判断从begin+1开始
                    // 此处不可以直接begin到position添加，虽然可以保证begin到position不是非法的，但是不能保证begin后面的字符到position是不是非法的
                    stringBuilder.append(word.charAt(begin));
                    position = ++begin;
                    template = root;
                } else if (template.isEnd()) {
                    // 如果此字符为敏感词最后一个字符，则将begin到position替换字符，begin到position+1开始判断
                    stringBuilder.append(SENSITIVEREPLACE);
                    begin = ++position;
                    template = root;
                } else {
                    // 若此字符存在敏感词中但不是敏感词最后一个,继续判断下一个
                    position++;
                }
            }
            // position遍历越界仍未匹配到敏感词
            else {
                stringBuilder.append(word.charAt(begin));
                position = ++begin;
                template = root;
            }
        }

        // 由于position比begin提前到末尾，此时可能会有未判断完的字符，但是没有剩余字符可以判断了，同时可以保证begin到？？？？
        stringBuilder.append(word.substring(begin));
        return stringBuilder.toString();
    }

    public boolean isSymbol(char c) {
        // 0x2E80~0x9FFF 是东亚文字范围不算符号
        return !CharUtils.isAsciiPrintable(c) && (c < 0x2E80 || c > 0x9FFF);
    }

}
