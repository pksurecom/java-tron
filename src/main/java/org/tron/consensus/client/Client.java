/*
 * java-tron is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * java-tron is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.tron.consensus.client;

import io.atomix.catalyst.transport.Address;
import io.atomix.catalyst.transport.netty.NettyTransport;
import io.atomix.copycat.client.ConnectionStrategies;
import io.atomix.copycat.client.CopycatClient;
import org.tron.consensus.common.GetQuery;
import org.tron.consensus.common.PutCommand;
import org.tron.overlay.message.Message;
import org.tron.overlay.message.Type;
import org.tron.peer.Peer;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public class Client {

    private static CopycatClient client = null;

    static {
        client = CopycatClient.builder()
                .withTransport(NettyTransport.builder()
                        .withThreads(2)
                        .build())
                .withConnectionStrategy(ConnectionStrategies.FIBONACCI_BACKOFF)
                .build();

        client.serializer().register(PutCommand.class);
        client.serializer().register(GetQuery.class);

        /*Collection<Address> cluster = Arrays.asList(
                new Address("192.168.0.100", 5000)
        );
        CompletableFuture<CopycatClient> future = client.connect(cluster);
        future.join();*/
        InetAddress localhost = null;
        try {
            localhost = InetAddress.getLocalHost();
            Collection<Address> cluster = Arrays.asList(
                    new Address(localhost.getHostAddress(), 5000)
            );

            CompletableFuture<CopycatClient> future = client.connect(cluster);
            future.join();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    public static CopycatClient getClient() {
        return client;
    }

    public static void putMessage(String[] args) {
        String key = args[0];
        String value = args[1];
        client.submit(new PutCommand(key, value));
        //client.submit(new PutCommand("time", System.currentTimeMillis()));
        System.out.println("Send message success");
    }

    public static void getMessage1(String key) {
        Object result = client.submit(new GetQuery(key)).join();
        System.out.println("Consensus " + key + " is: " + result);
    }

    public static void putMessage1(Message message) {
        if (message.getType() == Type.TRANSACTION) {
            /*
            System.out.println("transaction:" + message.getType().toString()
                    + "; type: " + message.getMessage().getClass().getSimpleName
                    () + "; message: " + message.getMessage()); */
            client.submit(new PutCommand("transaction", message.getMessage()));
            client.submit(new PutCommand("time", System.currentTimeMillis()));
            System.out.println("transaction: consensus success");
        }

        if (message.getType() == Type.BLOCK) {
            /*
            System.out.println("block:" + message.getType().toString()
                    + "; type: " + message.getMessage().getClass().getSimpleName
                    () + "; message:" + message.getMessage());*/

            //client.submit(new PutCommand("block", message.getMessage()));
            //System.out.println("Block: consensus success");

            int i = 1;
            final boolean[] f = {true};
            while (f[0]) {
                String block_key = "block" + i;
                Object block = client.submit(new GetQuery(block_key)).join();
                try {
                    if (!(block == null)) {
                        f[0] = true;
                        i = i + 1;
                    } else {
                        client.submit(new PutCommand(block_key, message
                                .getMessage()));
                        System.out.println("Block: consensus success");
                        f[0] = false;
                    }
                } catch (NullPointerException e) {
                    e.printStackTrace();
                    System.out.println("object == null");
                }
            }
        }
    }

    public static void getMessage(Peer peer, String key) {
        final String[] preMessage = {null};
        final String[] preTime = {null};
        if (key.equals("transaction")) {
            Thread thread = new Thread(() -> {
                while (true) {
                    Object time = client.submit(new GetQuery("time")).join();
                    if (!time.toString().equals(preTime[0])) {
                        client.submit(new GetQuery(key)).thenAccept(transaction
                                -> {
                            //System.out.println("Consensus " + key + " is: "
                            // + result);
                            //System.out.println("type: " + result.getClass()
                            // .getSimpleName());
                            peer.addReceiveTransaction(String.valueOf
                                    (transaction));
                        });
                        preTime[0] = time.toString();
                    } else {
                        preTime[0] = preTime[0];
                    }
                    try {
                        Thread.sleep(3000);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            thread.start();
        }

        if (key.equals("block")) {
            Thread thread = new Thread(() -> {
                while (true) {
                    int i = 1;
                    final boolean[] f = {true};
                    String block_key;
                    while (f[0]) {
                        block_key = "block" + i;
                        Object block = client.submit(new GetQuery(block_key))
                                .join();
                        try {
                            if (!(block == null)) {
                                f[0] = true;
                                i = i + 1;
                            } else {
                                f[0] = false;
                            }
                        } catch (NullPointerException e) {
                            e.printStackTrace();
                        }
                    }

                    i = i - 1;
                    String finalBlock_key = "block" + i;
                    client.submit(new GetQuery(finalBlock_key)).thenAccept
                            (block -> {
                        /*System.out.println("Consensus " + key + " is: " +
                        block);*/
                        if (!String.valueOf(block).equals(preMessage[0])) {
                            peer.addReceiveBlock(String.valueOf(block));
                            preMessage[0] = String.valueOf(block);
                        } else {
                            preMessage[0] = preMessage[0];
                        }
                    });
                    try {
                        Thread.sleep(3000);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            thread.start();
        }
    }

    public static void loadBlock(Peer peer) {
        int i = 2;
        final boolean[] f = {true};
        while (f[0]) {
            String block_key = "block" + i;
            client.submit(new GetQuery(block_key)).thenAccept((Object block)
                    -> {
                if (!(block == null)) {
                    peer.addReceiveBlock(String.valueOf
                            (block));
                    f[0] = true;
                } else {
                    f[0] = false;
                }
            });
            i++;
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
