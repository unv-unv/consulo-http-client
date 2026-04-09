package org.javamaster.httpclient.impl.inspection.fix;

import consulo.application.Application;
import consulo.fileEditor.FileEditorManager;
import consulo.language.editor.inspection.LocalQuickFix;
import consulo.language.editor.inspection.PriorityAction;
import consulo.language.editor.inspection.ProblemDescriptor;
import consulo.project.Project;
import org.javamaster.httpclient.NlsBundle;
import org.javamaster.httpclient.impl.action.addHttp.AddAction;
import org.javamaster.httpclient.impl.env.EnvFileService;
import org.javamaster.httpclient.impl.ui.HttpEditorTopForm;

import static org.javamaster.httpclient.impl.env.EnvFileService.*;

/**
 * @author yudong
 */
public class CreateEnvVariableQuickFix implements LocalQuickFix, PriorityAction {

    private final boolean isPrivate;
    private final String variableName;
    private final Priority priority;

    public CreateEnvVariableQuickFix(boolean isPrivate, String variableName, Priority priority) {
        this.isPrivate = isPrivate;
        this.variableName = variableName;
        this.priority = priority;
    }

    @Override
    public String getName() {
        String tip = isPrivate ? "private" : "";
        return NlsBundle.message("unsolved.variable", tip);
    }

    @Override
    public void applyFix(Project project, ProblemDescriptor descriptor) {
        createJsonProperty(project, variableName);
    }

    private void createJsonProperty(Project project, String variableName) {
        if (!Application.get().isDispatchThread()) {
            return;
        }

        HttpEditorTopForm topForm = HttpEditorTopForm.getSelectedEditorTopForm(project);
        if (topForm == null) {
            return;
        }

        String envFileName = isPrivate ? PRIVATE_ENV_FILE_NAME : ENV_FILE_NAME;

        String httpFileParentPath = topForm.getFile().getParent().getPath();

        var jsonFile = getEnvJsonFile(envFileName, httpFileParentPath, project);
        if (jsonFile == null) {
            AddAction.createAndReInitEnvCompo(isPrivate);
            topForm.setSelectedEnv("dev");
        } else {
            if (topForm.getSelectedEnv() == null) {
                return;
            }
            FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
            fileEditorManager.openFile(jsonFile.getVirtualFile(), true);
        }

        String selectEnv = topForm.getSelectedEnv();
        if (selectEnv == null) {
            return;
        }
        EnvFileService envFileService = EnvFileService.getService(project);

        envFileService.createEnvValue(variableName, selectEnv, httpFileParentPath, envFileName);
    }

    @Override
    public Priority getPriority() {
        return priority;
    }
}
