package ir.ac.um.AndroidInstrumenter.Analysis.Instruments;

public class StandardCodeGenerator {

    public static String menuItemClickCodeGenerator(String menuId, String codeBlock){
        String resultCode = "";
        resultCode = "menu.findItem(" + menuId + ")." +
                     "setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {\n" +
                     "\t\t\t@Override\n" +
                     "\t\t\tpublic boolean onMenuItemClick(MenuItem item) {\n";
        resultCode += codeBlock;
        if(!hasLastLineContainReturnStmt(codeBlock))
            resultCode += "\t\t\t\treturn false;\n";
        resultCode += "\t\t\t}\n" +
                      "\t\t});\n";
        return resultCode;
    }

    private static boolean hasLastLineContainReturnStmt(String codeBlock) {
        String[] lines = codeBlock.split("\n");
        if(lines[lines.length-1].contains("return"))
            return true;
        return false;
    }


}
