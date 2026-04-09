package org.javamaster.httpclient.impl.intent;

import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorPopupHelper;
import consulo.language.editor.intention.BaseIntentionAction;
import consulo.language.psi.PsiFile;
import consulo.language.util.IncorrectOperationException;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.httpClient.localize.HttpClientLocalize;
import consulo.ui.ex.popup.JBPopup;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.virtualFileSystem.VirtualFile;
import org.javamaster.httpclient.impl.env.EnvFileService;
import org.javamaster.httpclient.impl.ui.HttpEditorTopForm;
import org.javamaster.httpclient.parser.HttpFile;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Set;

/**
 * @author yudong
 */
public class HttpSwitchEnvironmentIntention extends BaseIntentionAction {
    @NotNull
    @Override
    public LocalizeValue getText() {
        return HttpClientLocalize.switchEnvironment();
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
        return file instanceof HttpFile;
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
        HttpEditorTopForm topForm = HttpEditorTopForm.getSelectedEditorTopForm(project);
        if (topForm == null) {
            return;
        }

        EnvFileService envFileService = EnvFileService.getService(project);
        if (file == null || file.getVirtualFile() == null) {
            return;
        }

        VirtualFile parent = file.getVirtualFile().getParent();
        if (parent == null) {
            return;
        }

        String path = parent.getPath();

        Set<String> envSet = envFileService.getPresetEnvSet(path);

        JBPopupFactory popupFactory = JBPopupFactory.getInstance();
        JBPopup popup = popupFactory.createPopupChooserBuilder(new ArrayList<>(envSet))
            .setItemChosenCallback(item -> topForm.setSelectedEnv(item))
            .createPopup();

        EditorPopupHelper.getInstance().showPopupInBestPositionFor(editor, popup);
    }
}
