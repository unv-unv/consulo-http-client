package org.javamaster.httpclient.impl.action;

import consulo.dataContext.DataContext;
import consulo.httpClient.localize.HttpClientLocalize;
import consulo.language.editor.DaemonCodeAnalyzer;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.action.ComboBoxAction;
import consulo.ui.ex.awt.action.ComboBoxButtonImpl;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import org.javamaster.httpclient.env.EnvFileService;
import org.javamaster.httpclient.env.Environment;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author yudong
 */
public class ChooseEnvironmentAction extends ComboBoxAction {
    private final VirtualFile myFile;
    private ComboBoxButtonImpl myComboBoxButton;
    private Environment mySelectedEnv = Environment.NO_ENVIRONMENT;

    private Presentation myPresentation;

    public ChooseEnvironmentAction(VirtualFile file) {
        myFile = file;
    }

    @Override
    public JComponent createCustomComponent(Presentation presentation, String place) {
        ComboBoxButtonImpl button = createComboBoxButton(presentation);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JLabel jLabel = new JLabel(HttpClientLocalize.env().get());
        jLabel.setPreferredSize(new Dimension(38, jLabel.getPreferredSize().height));
        jLabel.setHorizontalAlignment(SwingConstants.CENTER);

        JPanel panel = new JPanel(new BorderLayout());

        panel.add(jLabel, BorderLayout.WEST);

        panel.add(button, BorderLayout.CENTER);

        return panel;
    }

    @Nonnull
    @Override
    protected ComboBoxButtonImpl createComboBoxButton(Presentation presentation) {
        myPresentation = presentation;

        presentation.setDescription(HttpClientLocalize.envTooltip());

        presentation.setText(mySelectedEnv.displayName());

        myComboBoxButton = (ComboBoxButtonImpl) super.createComboBoxButton(presentation);
        myComboBoxButton.setBorder(null);

        return myComboBoxButton;
    }

    @Nonnull
    @Override
    protected ActionGroup createPopupActionGroup(JComponent jComponent) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected ActionGroup createPopupActionGroup(JComponent button, DataContext dataContext) {
        var project = dataContext.getData(Project.KEY);
        if (project == null) {
            return ActionGroup.EMPTY_GROUP;
        }

        String path = myFile.getParent() != null ? myFile.getParent().getPath() : null;
        if (path == null) {
            return ActionGroup.EMPTY_GROUP;
        }

        EnvFileService envFileService = EnvFileService.getService(project);

        List<AnAction> actions = new ArrayList<>();
        actions.add(new MyAction(Environment.NO_ENVIRONMENT));
        for (String presetEnv : envFileService.getPresetEnvSet(path)) {
            actions.add(new MyAction(Environment.of(presetEnv)));
        }

        return new DefaultActionGroup(actions);
    }

    public Environment getSelectedEnv() {
        return mySelectedEnv;
    }

    public void setSelectEnv(Environment env) {
        mySelectedEnv = env;

        if (myComboBoxButton != null) {
            myPresentation.setText(mySelectedEnv.displayName());
        }
    }

    private class MyAction extends AnAction {
        private final Environment myEnv;

        MyAction(Environment env) {
            super(env.displayName());
            myEnv = env;
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(AnActionEvent e) {
            setSelectEnv(myEnv);

            DaemonCodeAnalyzer.getInstance(e.getRequiredData(Project.KEY)).restart();
        }
    }
}
