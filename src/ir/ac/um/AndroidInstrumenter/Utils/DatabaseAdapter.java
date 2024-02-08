package ir.ac.um.AndroidInstrumenter.Utils;

import com.sun.jdi.event.ExceptionEvent;
import ir.ac.um.AndroidInstrumenter.widget.Widget;
import ir.ac.um.AndroidInstrumenter.widget.WidgetDescriptor;

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseAdapter {
    private String packageName;
    //private int packageId;
    private String db_userName;
    private String db_password;
    private String db_url;

    public DatabaseAdapter(String packageName){
        this.packageName = packageName;
        this.db_password = "";
        db_userName = "root";
        db_url = "jdbc:mysql://localhost:3306/?useSSL=false";
    }

    private boolean destroyDatabase(){
        try{
            Class.forName("com.mysql.jdbc.Driver");
            Connection connection = DriverManager.getConnection(db_url,db_userName, db_password);
            Statement stmt = connection.createStatement();
            String sql = "DROP DATABASE " + packageName;
            stmt.executeUpdate(sql);
        }catch(Exception e){
            e.printStackTrace();
            return false;
        }
            return true;
    }

    public boolean createDatabase(){
        try{
            Class.forName("com.mysql.jdbc.Driver");
            Connection connection = DriverManager.getConnection(db_url,db_userName, db_password);
            Statement stmt = connection.createStatement();
            int status = stmt.executeUpdate("CREATE DATABASE " + packageName);
            if(status > 0) {
                db_url = "jdbc:mysql://localhost:3306/" + packageName +"?useSSL=false";
                System.out.println("Database is created successfully !!!");

                if(!createPointsTypeTable()){
                    Utils.showMessage("There is a problem in createing PointTypeTable!!!");
                    destroyDatabase();
                    return false;
                }
                if(!createWidgetTable()){
                    Utils.showMessage("There is a problem in createing WidgetTable!!!");
                    destroyDatabase();
                    return false;
                }

                if(!createWidgetDescriptorTable()){
                    Utils.showMessage("There is a problem in createing DescriptorTable!!!");
                    destroyDatabase();
                    return false;
                }

                if(!createInvariantsTable()){
                    Utils.showMessage("There is a problem in createing InvariantsTable!!!");
                    destroyDatabase();
                    return false;
                }

                if(!createFirstPartOfInvariantTable()){
                    Utils.showMessage("There is a problem in createing FirstPartOfInvariantTable!!!");
                    destroyDatabase();
                    return false;
                }

                if(!createSecondPartOfInvariantTabale()){
                    Utils.showMessage("There is a problem in createing SecondPartOfInvariantTable!!!");
                    destroyDatabase();
                    return false;
                }

            }

        }catch(Exception e){
            e.printStackTrace();
        }
        return true;
    }

    private boolean createSecondPartOfInvariantTabale() {
        try{
            Class.forName("com.mysql.jdbc.Driver");
            Connection connection = DriverManager.getConnection(db_url,db_userName, db_password);
            Statement stmt = connection.createStatement();
            String sql  = "create table secondpartofinvariantdetail (" +
                    " secondPartId INT(11) PRIMARY KEY AUTO_INCREMENT," +
                    " destinationContext VARCHAR(100) NOT NULL," +
                    " destinationView VARCHAR(100) NOT NULL," +
                    " destinationViewAttribute VARCHAR(100) NOT NULL," +
                    " useOrigMethod TINYINT(1) NOT NULL," +
                    " firstPartId INT(11)," +
                    " viewId INT(11));";
            stmt.executeUpdate(sql);

        }catch(Exception e){
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private boolean createFirstPartOfInvariantTable() {
        try{
            Class.forName("com.mysql.jdbc.Driver");
            Connection connection = DriverManager.getConnection(db_url,db_userName, db_password);
            Statement stmt = connection.createStatement();
            String sql  = "create table firstpartofinvariantdetail (" +
                    " firstPartId INT(11) PRIMARY KEY AUTO_INCREMENT," +
                    " originalInvariant VARCHAR(500) NOT NULL ," +
                    " sourceContext VARCHAR(255) NOT NULL," +
                    " sourceView VARCHAR(100) NOT NULL," +
                    " SourceViewAttribute VARCHAR(100) NOT NULL," +
                    " relationOperator VARCHAR(3) NOT NULL," +
                    " mathOperator VARCHAR(3)," +
                    " content VARCHAR(200)," +
                    " Flag TINYINT(4) NOT NULL," +
                    " priority FLOAT NOT NULL," +
                    " invarId INT(11) NOT NULL," +
                    " viewId INT(11));";
            stmt.executeUpdate(sql);

        }catch(Exception e){
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private boolean createInvariantsTable() {
        try{
            Class.forName("com.mysql.jdbc.Driver");
            Connection connection = DriverManager.getConnection(db_url,db_userName, db_password);
            Statement stmt = connection.createStatement();
            String sql  = "create table invariantstable (" +
                    " invarId INT(11) PRIMARY KEY AUTO_INCREMENT," +
                    " invariant VARCHAR(500) NOT NULL ," +
                    " pointTypeId INT(100) NOT NULL);";
            stmt.executeUpdate(sql);

        }catch(Exception e){
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private boolean createWidgetDescriptorTable() {
        try{
            Class.forName("com.mysql.jdbc.Driver");
            Connection connection = DriverManager.getConnection(db_url,db_userName, db_password);
            Statement stmt = connection.createStatement();
            String sql  = "create table descriptor (" +
                    " descriptorId INT(11) PRIMARY KEY AUTO_INCREMENT," +
                    " widgetId INT(11) NOT NULL ," +
                    " descriptor VARCHAR(100) NOT NULL ," +
                    " value VARCHAR(100) NOT NULL);";
            stmt.executeUpdate(sql);

        }catch(Exception e){
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private boolean createWidgetTable() {
        try{
            Class.forName("com.mysql.jdbc.Driver");
            Connection connection = DriverManager.getConnection(db_url,db_userName, db_password);
            Statement stmt = connection.createStatement();
            String sql  = "create table widget (" +
                    " widgetId INT(11) PRIMARY KEY AUTO_INCREMENT," +
                    " ContextName VARCHAR(200) NOT NULL ," +
                    " WidgetType VARCHAR(100) NOT NULL ," +
                    " bindingVariable VARCHAR(100) NULL );";
            stmt.executeUpdate(sql);

        }catch(Exception e){
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private boolean createPointsTypeTable() {
        try{
        Class.forName("com.mysql.jdbc.Driver");
        Connection connection = DriverManager.getConnection(db_url,db_userName, db_password);
        Statement stmt = connection.createStatement();
        String sql  = "create table pointstypetable (" +
                " pointId INT(11) PRIMARY KEY AUTO_INCREMENT," +
                " pointType VARCHAR(100)," +
                " ParentContext VARCHAR(100)," +
                " Context VARCHAR(100)," +
                " MethodName VARCHAR(200)," +
                " caption  VARCHAR(255));";
        stmt.executeUpdate(sql);

        }catch(Exception e){
            e.printStackTrace();
            return false;
        }
        return true;
    }

//    public boolean prepare(){
//        String sql;
//        List<Integer> pointIdList = new ArrayList<>();
//        try {
//            Class.forName("com.mysql.jdbc.Driver");
//            Connection connection = DriverManager.getConnection(db_url,db_userName, db_password);
//            sql = "SELECT `packageId` from packagetable where packageName ='" + packageName + "'";
//            Statement statement = connection.createStatement();
//            ResultSet resultSet = statement.executeQuery(sql);
//            if(resultSet.next())
//                packageId = resultSet.getInt("packageId");
//            else{
//                sql = "INSERT INTO packagetable (packageName) VALUES ('" + packageName + "')";
//                statement.executeUpdate(sql);
//                sql = "SELECT packageId from packagetable where packageName ='" + packageName +"'";
//                resultSet = statement.executeQuery(sql);
//                if(resultSet.next())
//                    packageId = resultSet.getInt("packageId");
//                else
//                    return false;
//            }
//            statement.close();
//        } catch (Exception e) {
//            e.printStackTrace();
//            return false;
//        }
//        return true;
//    }

    public int appendWidget(String context, Widget widget) {
        int widgetId = 0;
        try (Connection connection = DriverManager.getConnection(db_url,
                db_userName, db_password);
             Statement stmt = connection.createStatement();){
                 widgetId = getWidgetIdFromDatabase(context,widget);
                 if(widgetId == -1){
                     String sql = "INSERT INTO widget (ContextName,WidgetType,bindingVariable) " +
                                   "VALUES ('" + context +"','" + widget.getWidgetType() +
                             "','" + widget.getBindingVariableName() +"')";
                     stmt.executeUpdate(sql);
                     sql = "SELECT * FROM widget ORDER BY widgetId DESC LIMIT 1" ;
                     ResultSet resultSet = stmt.executeQuery(sql);
                     resultSet.next();
                     widgetId = resultSet.getInt("widgetId");
                     appendDescriptors(widget,widgetId);
                 }
        }catch (SQLException ioe){
            ioe.printStackTrace();
            Utils.showMessage("There is a problem in working with the database!!!");
            widgetId = -1;
        }
        return widgetId;
    }

    private int getWidgetIdFromDatabase(String context, Widget widget) {

        try (Connection connection = DriverManager.getConnection(db_url,
                db_userName, db_password);
             Statement stmt = connection.createStatement();){

             String sql = "SELECT * FROM widget WHERE ContextName = '" + context + "' AND WidgetType = '" + widget.getWidgetType() + "'" +
                    " AND bindingVariable = '" + widget.getBindingVariableName() + "'";
             ResultSet resultSet = stmt.executeQuery(sql);
             List<Integer> widgetIds = new ArrayList<>();
             while(resultSet.next())
                widgetIds.add(resultSet.getInt("widgetId"));
             if(widgetIds.size() == 0)
                 return -1;
             for(Integer widgetId :widgetIds){
                 sql = "SELECT * from guiinvariants.descriptor where descriptor.widgetId=" + widgetId;
                 ResultSet descriptors = stmt.executeQuery(sql);
                 boolean flag = true;
                 while(descriptors.next()){
                     String descriptor = descriptors.getString("descriptor");
                     switch (descriptor){
                         case "ViewId" :
                             if(!widget.getWidgetIdDescriptorValue().contentEquals(descriptors.getString("value")))
                                 flag = false;
                             break;

                         case "ViewLabel" :
                             if(!widget.getWidgetLabelDescriptorValue().contentEquals(descriptors.getString("value")))
                                 flag = false;
                             break;

                         case "ViewHint" :
                             if(!widget.getWidgetHintDescriptorValue().contentEquals(descriptors.getString("value")))
                                 flag = false;
                             break;

                         case "ViewContentDescription":
                             if(!widget.getWidgetContentDescriptorValue().contentEquals(descriptors.getString("value")))
                                 flag = false;
                             break;

                     }
                     if(!flag)
                         break;

                 }
                 if(flag)
                     return widgetId;
             }
              return -1;
        }catch (SQLException ioe){
            ioe.printStackTrace();
            Utils.showMessage("There is a problem in working with the database!!!");
            return -1;
        }
    }

    public int appendDescriptors(Widget widget,int widgetId){
        int descriptorId = -1;
        try (Connection connection = DriverManager.getConnection(db_url,
                db_userName, db_password);
            Statement stmt = connection.createStatement();){
            for(WidgetDescriptor descriptor : widget.getWidgetDescriptorsList()){
                String sql = "INSERT INTO descriptor (widgetId,descriptor,value) VALUES ('"
                                 + widgetId + "','" + descriptor.getViewDescriptorType() + "','" +
                                 descriptor.getValue().replaceAll("\"","") +"')";
                stmt.executeUpdate(sql);
            }

        }catch (SQLException ioe){
            ioe.printStackTrace();
            Utils.showMessage("There is a problem in working with the database!!!");
            return -1;
        }
        return descriptorId;
    }

    private List<Widget> loadStoredGUIWidgets(String context){
        List<Widget> GUIWidgetList = new ArrayList<>();

        try (Connection connection = DriverManager.getConnection(db_url,
                db_userName, db_password);
             Statement stmt = connection.createStatement();){
            String sql = "SELECT * FROM widget WHERE ContextName ='" + context +"'";
            ResultSet widgetSet = stmt.executeQuery(sql);
            while(widgetSet.next()){
                Widget widget = new Widget();
                widget.setWidgetDatabaseId(widgetSet.getInt("widgetId"));
                widget.setWidgetType(widgetSet.getString("widgetType"));
                widget.setBindingVariable(widgetSet.getString("bindingVariable"));
                GUIWidgetList.add(widget);
            }
            for(Widget widget:GUIWidgetList){
                sql = "SELECT * FROM descriptor WHERE widgetId =" + widget.getWidgetDatabaseId();
                ResultSet descriptorSet = stmt.executeQuery(sql);
                while(descriptorSet.next()){
                    String descriptor = descriptorSet.getString("descriptor");
                    switch(descriptor){
                        case "ViewId": widget.setWidgetIdDescriptor(descriptorSet.getString("value"));break;
                        case "ViewLabel" : widget.setWidgetLabelDescriptor(descriptorSet.getString("value"));break;
                        case "ViewHint" : widget.setWidgetHintDescriptor(descriptorSet.getString("value"));break;
                    }
                }
            }
        }catch (SQLException ioe){
            ioe.printStackTrace();
            Utils.showMessage("There is a problem in working with the database!!!");
        }
        return GUIWidgetList;
    }

     public List<Widget> loadGUIWidgets(String context){
//        List<Widget> guiWidgetList = new ArrayList<>();
//
//        try (Connection connection = DriverManager.getConnection(db_url,
//                 db_userName, db_password);
//              Statement stmt = connection.createStatement();){
//              String sql =  "SELECT * FROM widget,descriptor WHERE widget.widgetId = descriptor.widgetId AND " +
//                            "ContextName ='" + context +"' AND descriptor.descriptor ='ViewId'" ;
//              ResultSet widgetSet = stmt.executeQuery(sql);
//              while(widgetSet.next()){
//                  Widget widget = new Widget();
//                  widget.setWidgetDatabaseId(widgetSet.getInt("widgetId"));
//                  widget.setWidgetType(widgetSet.getString("widgetType"));
//                  widget.setBindingVariable(widgetSet.getString("bindingVariable"));
//                  widget.setWidgetIdDescriptor(widgetSet.getString("value"));
//                  guiWidgetList.add(widget);
//              }
//
//        }catch (SQLException ioe){
//             ioe.printStackTrace();
//             Utils.showMessage("There is a problem in working with the database!!!");
//         }
//         return guiWidgetList;
         return loadStoredGUIWidgets(context);

     }

    public List<Widget> loadtmpGUIWidgets(String context){
//        List<Widget> tmpGUIWidgetList = new ArrayList<>();
//
//        try (Connection connection = DriverManager.getConnection(db_url,
//                db_userName, db_password);
//             Statement stmt = connection.createStatement();){
//            String sql = "SELECT * FROM widget WHERE ContextName ='" + context +"'";
//            ResultSet widgetSet = stmt.executeQuery(sql);
//            while(widgetSet.next()){
//                Widget widget = new Widget();
//                widget.setWidgetDatabaseId(widgetSet.getInt("widgetId"));
//                widget.setWidgetType(widgetSet.getString("widgetType"));
//                widget.setBindingVariable(widgetSet.getString("bindingVariable"));
//                tmpGUIWidgetList.add(widget);
//            }
//            for(Widget widget:tmpGUIWidgetList){
//                sql = "SELECT * FROM descriptor WHERE widgetId =" + widget.getWidgetDatabaseId();
//                ResultSet descriptorSet = stmt.executeQuery(sql);
//                while(descriptorSet.next()){
//                    String descriptor = descriptorSet.getString("descriptor");
//                    switch(descriptor){
//                        case "ViewId": widget.setWidgetIdDescriptor(descriptorSet.getString("value"));break;
//                        case "ViewLabel" : widget.setWidgetLabelDescriptor(descriptorSet.getString("value"));break;
//                        case "ViewHint" : widget.setWidgetHintDescriptor(descriptorSet.getString("value"));break;
//                    }
//                }
//            }
//        }catch (SQLException ioe){
//            ioe.printStackTrace();
//            Utils.showMessage("There is a problem in working with the database!!!");
//        }
//        return tmpGUIWidgetList;
          return loadStoredGUIWidgets(context);
    }






}
