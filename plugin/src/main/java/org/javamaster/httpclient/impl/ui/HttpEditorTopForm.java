package org.javamaster.httpclient.impl.ui;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import consulo.fileEditor.FileEditor;
import consulo.fileEditor.FileEditorManager;
import consulo.module.Module;
import consulo.project.Project;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.ActionToolbar;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.util.collection.ArrayUtil;
import consulo.util.dataholder.Key;
import consulo.util.lang.Trinity;
import consulo.virtualFileSystem.VirtualFile;
import org.javamaster.httpclient.env.Environment;
import org.javamaster.httpclient.impl.action.ChooseEnvironmentAction;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;
import java.util.Set;

/**
 * @author yudong
 */
public class HttpEditorTopForm extends JComponent {
    public static final Key<HttpEditorTopForm> KEY = Key.create("httpRequest.httpEditorTopForm");

    public final VirtualFile file;

    public JPanel mainPanel;

    private JPanel btnLeftPanel;
    private JPanel btnRightPanel;

    private final ChooseEnvironmentAction chooseEnvironmentAction;

    private final @Nullable Module module;

    public HttpEditorTopForm(VirtualFile file, @Nullable Module module, FileEditor fileEditor) {
        this.file = file;
        this.module = module;

        $$$setupUI$$$();

        ActionManager actionManager = ActionManager.getInstance();

        chooseEnvironmentAction = new ChooseEnvironmentAction(file);

        ActionGroup toolbarLeftBtnGroup = (ActionGroup) actionManager.getAction("httpToolbarLeftBtnGroup");
        assert toolbarLeftBtnGroup != null;

        DefaultActionGroup leftGroup = new DefaultActionGroup();
        leftGroup.addAll(toolbarLeftBtnGroup);
        leftGroup.addSeparator();
        leftGroup.add(chooseEnvironmentAction);

        ActionToolbar toolbarLeft = actionManager.createActionToolbar("httpRequestLeftToolbar", leftGroup, true);

        toolbarLeft.setTargetComponent(fileEditor.getComponent());

        btnLeftPanel.add(toolbarLeft.getComponent(), BorderLayout.CENTER);

        ActionGroup toolbarRightBtnGroup = (ActionGroup) actionManager.getAction("httpToolbarRightBtnGroup");
        assert toolbarRightBtnGroup != null;

        ActionToolbar toolbarRight = actionManager.createActionToolbar("HttpRequestRightToolbar", toolbarRightBtnGroup, true);

        toolbarRight.setTargetComponent(fileEditor.getComponent());

        btnRightPanel.add(toolbarRight.getComponent(), BorderLayout.CENTER);
    }

    public VirtualFile getFile() {
        return file;
    }

    public void initEnvCombo(Set<String> presetEnvSet) {
        if (presetEnvSet.contains("uat")) {
            setSelectedEnv("uat");
        }
        else if (presetEnvSet.contains("test")) {
            setSelectedEnv("test");
        }
    }

    public @Nullable String getSelectedEnv() {
        return chooseEnvironmentAction.getSelectedEnv().value();
    }

    public void setSelectedEnv(@Nullable String env) {
        chooseEnvironmentAction.setSelectEnv(Environment.of(env));
    }

    public static @Nullable String getSelectedEnv(Project project) {
        HttpEditorTopForm httpEditorTopForm = getSelectedEditorTopForm(project);
        if (httpEditorTopForm == null) {
            return null;
        }

        return httpEditorTopForm.getSelectedEnv();
    }

    public static @Nullable Trinity<@Nullable String, VirtualFile, Module> getTriple(Project project) {
        HttpEditorTopForm topForm = getSelectedEditorTopForm(project);
        if (topForm == null) {
            return null;
        }

        return new Trinity<>(topForm.getSelectedEnv(), topForm.file, topForm.module);
    }

    public static @Nullable HttpEditorTopForm getSelectedEditorTopForm(Project project) {
        FileEditorManager editorManager = FileEditorManager.getInstance(project);
        FileEditor selectedEditor = ArrayUtil.getFirstElement(editorManager.getSelectedEditors());
        if (selectedEditor == null) {
            return null;
        }

        return selectedEditor.getUserData(HttpEditorTopForm.KEY);
    }

    public static void setCurrentEditorSelectedEnv(String httpFilePath, Project project, String env) {
        FileEditorManager editorManager = FileEditorManager.getInstance(project);
        FileEditor selectedEditor = ArrayUtil.getFirstElement(editorManager.getSelectedEditors());
        if (selectedEditor == null) {
            return;
        }

        VirtualFile virtualFile = selectedEditor.getFile();
        if (virtualFile == null || !Objects.equals(httpFilePath, virtualFile.getPath())) {
            return;
        }

        HttpEditorTopForm httpEditorTopForm = selectedEditor.getUserData(HttpEditorTopForm.KEY);
        if (httpEditorTopForm == null) {
            return;
        }

        httpEditorTopForm.setSelectedEnv(env);
    }

    private void $$$setupUI$$$() {
        mainPanel = new JPanel();
        mainPanel.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.setToolTipText("");
        final Spacer spacer1 = new Spacer();
        mainPanel.add(spacer1, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        btnLeftPanel = new JPanel();
        btnLeftPanel.setLayout(new BorderLayout(0, 0));
        mainPanel.add(btnLeftPanel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        btnRightPanel = new JPanel();
        btnRightPanel.setLayout(new BorderLayout(0, 0));
        mainPanel.add(btnRightPanel, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return mainPanel;
    }
}
