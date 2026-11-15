package pitiupi.batalhanaval.plugin;

import pitiupi.api.plugin.PeerActionsRegistrar;
import pitiupi.api.plugin.PluginWorkspace;
import pitiupi.application.peeractions.PeerAction;
import pitiupi.application.peeractions.PeerActionTarget;
import pitiupi.application.peeractions.PeerTarget;
import pitiupi.batalhanaval.GUI.BatalhaNavalPanel;
import pitiupi.batalhanaval.net.BatalhaMessage;
import pitiupi.control.ConnectionManager;
import pitiupi.models.Messages.Message;
import pitiupi.models.Plugin;

import javax.swing.SwingUtilities;
import java.util.List;

public class BatalhaNavalPlugin extends Plugin {

    private BatalhaNavalPanel panel;
    private PluginWorkspace workspace;

    @Override
    public void createPlugin(ConnectionManager connectionManager, PluginWorkspace workspace) {
        this.workspace = workspace;
        this.panel = new BatalhaNavalPanel(connectionManager);
        bindShellWorkspaceContent(panel);
        installPeerActions(workspace.peerActions());
    }

    @Override
    public void installPeerActions(PeerActionsRegistrar registrar) {
        registrar.register(PeerAction.of(
                "batalhanaval.desafiar",
                "Desafiar para Batalha Naval",
                PeerActionTarget.SINGLE_PEER,
                target -> {
                    if (target instanceof PeerTarget.Single single) {
                        workspace.openOrFocusTab(getIdentifier(), getDisplayName());
                        panel.desafiarPeer(single.peer());
                    }
                }
        ));
    }

    @Override
    public List<Class<? extends Message>> getMessages() {
        return List.of(BatalhaMessage.class);
    }

    @Override
    public void receiveMessage(Message message) {
        if (message instanceof BatalhaMessage msg && panel != null) {
            SwingUtilities.invokeLater(() -> panel.processarMensagem(msg));
        }
    }

    @Override
    public void onShellTabClosed() {
        if (panel != null) {
            panel.encerrarPartidaPorFechamento();
        }
    }

    @Override
    public String getName() {
        return "Batalha Naval";
    }

    @Override
    public String getDisplayName() {
        return "Batalha Naval P2P";
    }

    @Override
    public String getAuthor() {
        return "Rafael Dayvid";
    }

    @Override
    public String getVersion() {
        return "1.0";
    }

    @Override
    public String getIdentifier() {
        return "pitiupi.batalhanaval";
    }
}
