package emu.grasscutter.server.packet.send;

import emu.grasscutter.net.packet.*;
import com.google.protobuf.CodedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class PacketHomeUpdateArrangementInfoRsp extends BasePacket {

    public PacketHomeUpdateArrangementInfoRsp() {
        super(PacketOpcodes.HomeUpdateArrangementInfoRsp);
    }

    public PacketHomeUpdateArrangementInfoRsp(int retcode) {
        super(PacketOpcodes.HomeUpdateArrangementInfoRsp);
        try {
            var bytes = new ByteArrayOutputStream();
            var output = CodedOutputStream.newInstance(bytes);
            output.writeInt32(1, retcode);
            output.flush();
            this.setData(bytes.toByteArray());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
