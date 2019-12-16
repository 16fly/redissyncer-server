package syncer.syncerservice.cmd;


import lombok.Builder;
import redis.clients.jedis.commands.ProtocolCommand;

/**
 * 命令模块
 */
@Builder
public class ClusterProtocolCommand implements ProtocolCommand {
    private byte[] raw;

    public ClusterProtocolCommand(byte[] raw) {
        this.raw = raw;
    }


    @Override
    public byte[] getRaw() {
        return this.raw;
    }
}
