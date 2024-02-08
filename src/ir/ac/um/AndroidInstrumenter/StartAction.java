package ir.ac.um.AndroidInstrumenter;

import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.file.PsiJavaDirectoryImpl;
import com.intellij.ui.content.Content;
import ir.ac.um.AndroidInstrumenter.Analysis.Project.ProjectInformation;
import ir.ac.um.AndroidInstrumenter.Utils.BackupCreator;
import ir.ac.um.AndroidInstrumenter.Utils.Constants;
import ir.ac.um.AndroidInstrumenter.Utils.FileUtils;
import ir.ac.um.AndroidInstrumenter.Utils.Utils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;

public class StartAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
        Project project = anActionEvent.getProject();
        PsiElement projectElement = anActionEvent.getData(LangDataKeys.PSI_ELEMENT);
        processProject(project,projectElement);
    }

    private void processProject(Project project,PsiElement projectElement) {
        ProjectInformation projectInformation = new ProjectInformation(project,projectElement);
        projectInformation.collectInformation();
        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(Constants.PLUGIN_NAME);
        ConsoleView consoleView = Utils.getConsoleView();
        if (consoleView == null) {
            consoleView = TextConsoleBuilderFactory.getInstance().createBuilder(project).getConsole();
            Utils.setConsoleView(consoleView);
            Content content = toolWindow.getContentManager().getFactory().createContent(consoleView.getComponent(), Constants.PLUGIN_NAME, true);
            toolWindow.getContentManager().addContent(content);
        }
        toolWindow.show(null);
        Utils.showMessage("Started processing project " + project.getName());
       // prepareAppForInstrumenting();
        ApplicationManager.getApplication().executeOnPooledThread(() ->
                ApplicationManager.getApplication().runReadAction(new Instrumenter(project, projectElement,projectInformation))
        );
        Utils.showMessage("Finished");
    }

    @Override
    public void update(AnActionEvent anActionEvent) {
        Project project = anActionEvent.getProject();
        PsiElement psiElement = anActionEvent.getData(LangDataKeys.PSI_ELEMENT);
        boolean enabled = project != null && (psiElement instanceof PsiJavaDirectoryImpl)
                && ((PsiDirectory) psiElement).getVirtualFile().getCanonicalPath().equals(
                project.getBasePath());
        anActionEvent.getPresentation().setEnabledAndVisible(enabled);
    }
}
