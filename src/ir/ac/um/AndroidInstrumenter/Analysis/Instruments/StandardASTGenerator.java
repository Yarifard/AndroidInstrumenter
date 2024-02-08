package ir.ac.um.AndroidInstrumenter.Analysis.Instruments;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.Statement;
import ir.ac.um.AndroidInstrumenter.Analysis.Project.ProjectInformation;
import ir.ac.um.AndroidInstrumenter.Utils.ASTUtils;

import java.util.ArrayList;
import java.util.List;

public  class StandardASTGenerator {
    private ProjectInformation projectInformation;
    private final CompilationUnit standardAST;
   // private ActivityInformation activityInformation;
    private TypeDeclaration activityClassNode;
    private String mainLayoutFileName;
    private List<String> includedLayouts = new ArrayList<>();

    public StandardASTGenerator(ProjectInformation projectInformation,CompilationUnit originalAST, String mainLayout,
                                List<String> includedLayouts) {
        this.projectInformation = projectInformation;
        this.standardAST = originalAST;
        this.mainLayoutFileName = mainLayout;
        this.includedLayouts = includedLayouts;
        initialActivityClassNode();
        performTransformation();
    }

    public void initialActivityClassNode() {
        standardAST.getTypes().forEach(node -> {
            if (node.getClass().toString().contains("ClassOrInterfaceDeclaration"))
                activityClassNode = node;
        });
    }

    private void initialLayouts(){

    }


    private void performTransformation() {
        performStandardMenuTransform();
        performContextMenuTransform();
        PerformEventHandlerTransform();
    }

    public CompilationUnit get() {
        return standardAST;
    }

    private void appendMainMenuEventHandlersCodeBlocks(Node onCreateMainMenuMethod, List<CodeBlock> codeBlocks) {
        String menulayout = "";
        MethodDeclaration menuMethod = (MethodDeclaration) onCreateMainMenuMethod;
        BlockStmt targetBlock = ASTUtils.findTargetBlock(menuMethod);
        if(ASTUtils.menuHasLayout(menuMethod))
             menulayout = ASTUtils.extrctMenuLayout(menuMethod);
        Statement stmt;
        for(CodeBlock codeBlock:codeBlocks){
             stmt = StaticJavaParser.parseStatement(codeBlock.code);
             if(!menulayout.isEmpty()){
                if(ASTUtils.isDefinedInMenuLayout(projectInformation,menulayout,codeBlock.itemId)){
                    if(targetBlock.getStatements().size() > 1)
                       targetBlock.addStatement(targetBlock.getStatements().size() - 1,stmt);
                    else
                        targetBlock.addStatement(targetBlock.getStatements().size(),stmt);
                }
                else
                    menuMethod.getBody().get().addStatement(menuMethod.getBody().get().getStatements().size() -1,stmt);
             }
             else
                 menuMethod.getBody().get().addStatement(menuMethod.getBody().get().getStatements().size() -1,stmt);
        }
    }

    private void appendContextMenuEventHandlerCode(Node onCreateContextMenuMethod, String code) {
        MethodDeclaration onCreatecontextMenuMethod = (MethodDeclaration) onCreateContextMenuMethod;
        Statement stmt;
        BlockStmt blockStmt = StaticJavaParser.parseBlock("{\n" + code + "\n}\n");
        for (int index = 0; index < blockStmt.getStatements().size(); index++) {
            stmt = blockStmt.getStatement(index);
            onCreatecontextMenuMethod.getBody().get().addStatement(onCreatecontextMenuMethod.getBody().get().getStatements().size(), stmt);
        }
    }


    private String getBeginCommonCode(MethodDeclaration method) {
        String beginCommonCode = "";
        BlockStmt blockStmt = method.getBody().get().asBlockStmt();
        int index = 0;
        for (Statement stmt : blockStmt.getStatements())
            if (!(ASTVisitor.visitIfStmt(stmt) || ASTVisitor.visitSwitchStmt(stmt)))
                beginCommonCode += stmt.toString() + "\n";
            else
                break;
        return beginCommonCode;
    }

    private String getEndCommonCode(MethodDeclaration method) {
        String endCommonCode = "";
        BlockStmt blockStmt = method.getBody().get().asBlockStmt();
        boolean flag = true;
        int index = 0;
        for (index = 0; index < blockStmt.getStatements().size(); index++)
            if (ASTVisitor.visitIfStmt(blockStmt.getStatement(index))||
                ASTVisitor.visitSwitchStmt(blockStmt.getStatement(index))) {
                flag = true;
                index++;
                break;
            }
        for (; index < blockStmt.getStatements().size(); index++){
              if(blockStmt.getStatement(index).isReturnStmt() && index == (blockStmt.getStatements().size() - 1))
                  continue;
              endCommonCode += blockStmt.getStatement(index).toString() + "\n";
        }
        return endCommonCode;
    }

    private List<CodeBlock> preprocess(List<CodeBlock> codeBlocks) {
        for (CodeBlock codeBlock : codeBlocks) {
            if ((codeBlock.code.startsWith("{") && codeBlock.code.endsWith("}")) ||
                (codeBlock.code.startsWith("[") && codeBlock.code.endsWith("]"))) {
                codeBlock.code = codeBlock.code.substring(1, codeBlock.code.length() - 1);
            }

            if(codeBlock.code.contains(";,"))
                codeBlock.code = codeBlock.code.replaceAll(";,",";");
            if(codeBlock.code.contains("},"))
                codeBlock.code = codeBlock.code.replaceAll("},","}\n");
            if(codeBlock.code.contains(";"))
                codeBlock.code = codeBlock.code.replaceAll(";",";\n");
            if(codeBlock.code.contains("break;"))
                codeBlock.code = codeBlock.code.replaceAll("break;","\n");
            if (codeBlock.itemId.contains("==")) {
                String[] parts = codeBlock.itemId.split("==");
                if (parts[0].contains("getItemId()") || parts[0].contains("getViewId()"))
                    codeBlock.itemId = parts[1].trim();
                else
                    codeBlock.itemId = parts[0].trim();
            }


        }
        return codeBlocks;
    }

    private List<CodeBlock> extactCodeBlocks(Node sourceMethod) {
        List<CodeBlock> codeBlocks = new ArrayList<>();
        String preFixCode = "", postFixCode = "";
        MethodDeclaration method = (MethodDeclaration) sourceMethod;
        if (ASTVisitor.visitIfStmt(method)) {
            preFixCode = getBeginCommonCode(method);
            postFixCode = getEndCommonCode(method);
            codeBlocks = preprocess(ASTVisitor.visitIfStmtSections(method));
            for (CodeBlock codeBlock : codeBlocks)
                codeBlock.code = preFixCode + codeBlock.code + postFixCode;
        } else if (ASTVisitor.visitSwitchStmt(method)) {
            preFixCode = getBeginCommonCode(method);
            postFixCode = getEndCommonCode(method);
            codeBlocks = preprocess(ASTVisitor.visitSwitchStmtEntries(method));
            for (CodeBlock codeBlock : codeBlocks)
                codeBlock.code = preFixCode + codeBlock.code + postFixCode;
        }
        return codeBlocks;
    }

    private void performContextMenuTransform() {
        String code = "";
        if (ASTVisitor.isExistContextMenuElement(activityClassNode))
            if (!ASTVisitor.isStandardContextMenu(activityClassNode)) {
                Node onCreateContextMenuMethod = ASTVisitor.getOnCreateContextMenu();
                Node OnContextItemSelectedMethod = ASTVisitor.getOnContextItemSelected();
                List<CodeBlock> codeBlockList = extactCodeBlocks(OnContextItemSelectedMethod);
                for (CodeBlock codeBlock : codeBlockList)
                    code += StandardCodeGenerator.menuItemClickCodeGenerator(codeBlock.itemId, codeBlock.code);
                appendContextMenuEventHandlerCode(onCreateContextMenuMethod, code);
                OnContextItemSelectedMethod.remove();
            }
    }

    private void performStandardMenuTransform() {
        String code = "";
        if (ASTVisitor.isExistMainMenuElement(activityClassNode))
            if (!ASTVisitor.isStandardMainMenu(activityClassNode)) {
                Node onCreateMainMenuMethod = ASTVisitor.getOnCreateOptionsMenuMethod();
                Node OnOptionsItemSelectedMethod = ASTVisitor.getOnOptionsItemSelectedMethod();
                List<CodeBlock> codeBlockList = extactCodeBlocks(OnOptionsItemSelectedMethod);
                for (CodeBlock codeBlock : codeBlockList)
                    codeBlock.code= StandardCodeGenerator.menuItemClickCodeGenerator(codeBlock.itemId, codeBlock.code);
                appendMainMenuEventHandlersCodeBlocks(onCreateMainMenuMethod, codeBlockList);
                OnOptionsItemSelectedMethod.remove();
            }
    }

    private void PerformEventHandlerTransform() {


    }

}
