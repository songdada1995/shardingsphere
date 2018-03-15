/*
 * Copyright 1999-2015 dangdang.com.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * </p>
 */

package io.shardingjdbc.proxy.codec;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;
import io.shardingjdbc.proxy.packet.MySQLPacket;
import io.shardingjdbc.proxy.packet.MySQLPacketPayload;
import io.shardingjdbc.proxy.packet.MySQLSentPacket;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * MySQL packet codec.
 * 
 * @author zhangliang 
 */
@Slf4j
public final class MySQLPacketCodec extends ByteToMessageCodec<MySQLSentPacket> {
    
    @Override
    protected void decode(final ChannelHandlerContext context, final ByteBuf in, final List<Object> out) throws Exception {
        int readableBytes = in.readableBytes();
        if (readableBytes < MySQLPacket.PAYLOAD_LENGTH + MySQLPacket.SEQUENCE_LENGTH) {
            return;
        }
        if (log.isDebugEnabled()) {
            log.debug("Read from client: \n {}", ByteBufUtil.prettyHexDump(in));
        }
        int payloadLength = in.markReaderIndex().readMediumLE();
        int realPacketLength = payloadLength + MySQLPacket.PAYLOAD_LENGTH + MySQLPacket.SEQUENCE_LENGTH;
        if (readableBytes < realPacketLength) {
            in.resetReaderIndex();
            return;
        }
        if (readableBytes > realPacketLength) {
            out.add(in.readRetainedSlice(payloadLength + MySQLPacket.SEQUENCE_LENGTH));
            return;
        }
        out.add(in);
    }
    
    @Override
    protected void encode(final ChannelHandlerContext context, final MySQLSentPacket message, final ByteBuf out) throws Exception {
        MySQLPacketPayload mysqlPacketPayload = new MySQLPacketPayload(context.alloc().buffer());
        message.write(mysqlPacketPayload);
        out.writeMediumLE(mysqlPacketPayload.getByteBuf().readableBytes());
        out.writeByte(message.getSequenceId());
        out.writeBytes(mysqlPacketPayload.getByteBuf());
        if (log.isDebugEnabled()) {
            log.debug("Write to client: \n {}", ByteBufUtil.prettyHexDump(out));
        }
    }
}
