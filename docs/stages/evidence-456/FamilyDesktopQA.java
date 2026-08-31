import com.kfaino.collector.desktop.storage.DesktopDataStore;
import com.kfaino.collector.desktop.server.EmbeddedWebServer;
import com.kfaino.collector.desktop.ui.DesktopWorkbench;
import java.io.File;
import java.nio.file.Files;
import org.json.*;
import javax.swing.*;

// Isolated data and loopback only. Output tokens are short-lived QA credentials in build/.
class FamilyDesktopQA {
    public static void main(String[] args) throws Exception {
        File directory=new File(args[0]); directory.mkdirs();
        DesktopDataStore store=new DesktopDataStore(directory);
        EmbeddedWebServer server=new EmbeddedWebServer(store,0,false,java.util.UUID.randomUUID().toString());
        var viewer=server.getFamilyAccess().issue("QA viewer","viewer");
        var editor=server.getFamilyAccess().issue("QA editor","editor");
        var shared=new JSONArray().put(viewer.getMember().getId()).put(editor.getMember().getId());
        store.importJson(new JSONObject().put("entries",new JSONArray()
            .put(new JSONObject().put("id","family-qa").put("brand","QA coffee").put("loc","Kitchen").put("_sharedWith",shared).put("secretField","must-not-leak"))
            .put(new JSONObject().put("id","private-qa").put("brand","QA book").put("loc","Study"))).toString());
        server.start();
        Files.writeString(new File(directory,"qa-connection.json").toPath(),new JSONObject().put("port",server.getBoundPort()).put("viewer",viewer.getToken()).put("editor",editor.getToken()).toString());
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
        if(args.length>1 && args[1].equals("--headless")) {new java.util.concurrent.CountDownLatch(1).await();return;}
        SwingUtilities.invokeLater(()-> {
            JFrame parent=new JFrame("Collecter QA456");
            parent.setSize(360,160); parent.setVisible(true);
            DesktopWorkbench window=new DesktopWorkbench(store,server.getFamilyAccess(),server.getBoundPort(),parent);
            window.addWindowListener(new java.awt.event.WindowAdapter(){public void windowClosed(java.awt.event.WindowEvent event){server.stop();parent.dispose();System.exit(0);}});
            window.setVisible(true);
        });
    }
}
