package ru.caramel.juniperbot.audio.service;

import net.dv8tion.jda.core.audio.AudioConnection;
import net.dv8tion.jda.core.audio.factory.DefaultSendSystem;
import net.dv8tion.jda.core.audio.factory.IAudioSendFactory;
import net.dv8tion.jda.core.audio.factory.IAudioSendSystem;
import net.dv8tion.jda.core.audio.factory.IPacketProvider;
import net.dv8tion.jda.core.managers.impl.AudioManagerImpl;
import net.dv8tion.jda.core.utils.JDALogger;
import org.springframework.stereotype.Service;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.NoRouteToHostException;
import java.net.SocketException;

import static net.dv8tion.jda.core.audio.AudioConnection.OPUS_FRAME_TIME_AMOUNT;

@Service
public class AudioSendFactory implements IAudioSendFactory {

    @Override
    public IAudioSendSystem createSendSystem(IPacketProvider packetProvider) {
        return new AudioSendSystem(packetProvider);
    }

    private static class AudioSendSystem implements IAudioSendSystem {
        private final IPacketProvider packetProvider;
        private Thread sendThread;

        private AudioSendSystem(IPacketProvider packetProvider) {
            this.packetProvider = packetProvider;
        }

        @Override
        public void start() {
            final DatagramSocket udpSocket = packetProvider.getUdpSocket();

            sendThread = new Thread(AudioManagerImpl.AUDIO_THREADS, () ->
            {
                long lastFrameSent = System.currentTimeMillis();
                while (!udpSocket.isClosed() && !sendThread.isInterrupted()) {
                    try {
                        boolean changeTalking = (System.currentTimeMillis() - lastFrameSent) > OPUS_FRAME_TIME_AMOUNT;
                        DatagramPacket packet = packetProvider.getNextPacket(changeTalking);

                        if (packet != null)
                            udpSocket.send(packet);
                    } catch (NoRouteToHostException e) {
                        packetProvider.onConnectionLost();
                    } catch (SocketException e) {
                        //Most likely the socket has been closed due to the audio connection be closed. Next iteration will kill loop.
                    } catch (Exception e) {
                        AudioConnection.LOG.error("Error while sending udp audio data", e);
                    } finally {
                        long sleepTime = (OPUS_FRAME_TIME_AMOUNT) - (System.currentTimeMillis() - lastFrameSent);
                        if (sleepTime > 0) {
                            try {
                                Thread.sleep(sleepTime);
                            } catch (InterruptedException e) {
                                //We've been asked to stop.
                                Thread.currentThread().interrupt();
                            }
                        }
                        if (System.currentTimeMillis() < lastFrameSent + 60) // If the sending didn't took longer than 60ms (3 times the time frame)
                        {
                            lastFrameSent += OPUS_FRAME_TIME_AMOUNT; // increase lastFrameSent
                        } else {
                            lastFrameSent = System.currentTimeMillis(); // else reset lastFrameSent to current time
                        }
                    }
                }
            });
            sendThread.setUncaughtExceptionHandler((thread, throwable) ->
            {
                JDALogger.getLog(DefaultSendSystem.class).error("Uncaught exception in audio send thread", throwable);
                start();
            });
            sendThread.setDaemon(true);
            sendThread.setName(packetProvider.getIdentifier() + " Sending Thread");
            sendThread.setPriority(Thread.MAX_PRIORITY);
            sendThread.start();
        }

        @Override
        public void shutdown() {
            if (sendThread != null)
                sendThread.interrupt();
        }
    }
}
