package com.ficat.plugin;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collections;

public class IdSuffixInsert extends AnAction {
    private JDialog jDialog;
    private JTextField suffixJTextField;

    @Override
    public void actionPerformed(AnActionEvent anActionEvent) {
        Editor editor = anActionEvent.getData(PlatformDataKeys.EDITOR);
        Project project = anActionEvent.getData(PlatformDataKeys.PROJECT);
        if (editor == null || project == null) {
            return;
        }
        showDialog(project, editor);
    }

    private void showDialog(Project project, Editor editor) {
        jDialog = new JDialog();// 定义一个窗体Container container = getContentPane();
        jDialog.setModal(true);
        Container container = jDialog.getContentPane();
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));

        JPanel jPanel1 = new JPanel();// 创建面板1
        jPanel1.setLayout(new GridLayout(1, 1));
        jPanel1.setBorder(BorderFactory.createTitledBorder("输入后缀名"));
        suffixJTextField = new JTextField();
        jPanel1.add(suffixJTextField);
        container.add(jPanel1);


        JPanel jPanel2 = new JPanel();//创建面板2
        jPanel2.setLayout(new FlowLayout());
        JButton cancle = new JButton();
        cancle.setText("取消");
        cancle.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (jDialog != null) {
                    jDialog.dispose();
                }
            }
        });
        JButton commit = new JButton();
        commit.setText("确定");
        commit.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (jDialog != null) {
                    //得到输入的后缀名
                    String suffix = suffixJTextField.getText().trim();
                    jDialog.dispose();
                    if (editor == null || project == null) {
                        return;
                    }
                    Runnable r = new Runnable() {
                        @Override
                        public void run() {
                            insertSuffix(suffix, editor.getDocument());
                        }
                    };
                    //加入任务，由IDEA调度执行这个任务
                    WriteCommandAction.runWriteCommandAction(project, r);
                }
            }
        });
        jPanel2.add(cancle);
        jPanel2.add(commit);
        container.add(jPanel2);

        jDialog.setSize(240, 150);
        jDialog.setLocationRelativeTo(null);
        jDialog.setVisible(true);
    }

    /**
     * 插入后缀
     */
    private void insertSuffix(String suffix, Document doc) {
        if (doc == null || suffix == null || suffix.length() <= 0) {
            return;
        }
        String docText = doc.getText();
        ArrayList<Integer> offsetList = new ArrayList<>();
        String s1 = "@+id/";
        String s2 = "@id/";
        getInsertIndext(s1, suffix, docText, doc, offsetList);
        getInsertIndext(s2, suffix, docText, doc, offsetList);
        Collections.sort(offsetList);//从小到大排序
        for (int i = 0; i < offsetList.size(); i++) {
            //插入后缀
            doc.insertString(offsetList.get(i) + suffix.length() * i, suffix);
        }
    }

    private void getInsertIndext(String tag, String suffix, String docText, Document doc, ArrayList<Integer> offsetList) {
        int fromIndext = 0;
        while (true) {
            int indext = docText.indexOf(tag, fromIndext);
            if (indext == -1) {
                break;
            }
            //根据索引获取含id所在行的行号
            int line = doc.getLineNumber(indext);
            //根据行号获取该行的最前、最后字符在全文中的偏移量（最后一位为换行符）
            int indexStart = doc.getLineStartOffset(line);
            int indexLast = doc.getLineEndOffset(line);
            //获取该行字符串（注意：包括了换行符）
            String lineText = docText.substring(indexStart, indexLast + 1);
            //获取该行的最后一个分号在该行中的索引
            int position = lineText.indexOf('"', lineText.indexOf('"') + 1);

            String str = lineText.substring(0, position);
            //判断id末尾是否已加了该后缀，防止重复添加
            if (!str.endsWith(suffix)) {
                int insertPosition = indexStart + position;
                offsetList.add(insertPosition);
            }

            fromIndext = indext + tag.length();
        }
    }

    @Override
    public void update(AnActionEvent e) {
        super.update(e);
        Editor editor = e.getData(PlatformDataKeys.EDITOR);
        //默认不显示插件
        e.getPresentation().setVisible(false);
        //若当前处于layout.mxl文件，则显示插件
        if (editor != null && editor.getDocument() != null) {
            String text = editor.getDocument().getText();
            if (text != null && text.length() > 0) {
                String str = "xmlns:android=\"http://schemas.android.com/apk/res/android\"";
                if (text.contains(str)) {
                    e.getPresentation().setVisible(true);
                }
            }
        }
//            editor.getDocument().addDocumentListener(new DocumentListener() {
//                @Override
//                public void beforeDocumentChange(DocumentEvent event) {
//                }
//
//                @Override
//                public void documentChanged(DocumentEvent event) {
//
//                }
//            });
    }

}
