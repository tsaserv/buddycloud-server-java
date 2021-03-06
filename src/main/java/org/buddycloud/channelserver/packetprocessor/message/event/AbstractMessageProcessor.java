package org.buddycloud.channelserver.packetprocessor.message.event;

import java.util.Properties;
import java.util.concurrent.BlockingQueue;

import org.buddycloud.channelserver.Configuration;
import org.buddycloud.channelserver.channel.ChannelManager;
import org.buddycloud.channelserver.packetprocessor.PacketProcessor;
import org.buddycloud.channelserver.pubsub.affiliation.Affiliations;
import org.buddycloud.channelserver.pubsub.model.NodeMembership;
import org.buddycloud.channelserver.pubsub.subscription.Subscriptions;
import org.buddycloud.channelserver.utils.NotificationScheme;
import org.xmpp.packet.JID;
import org.xmpp.packet.Message;
import org.xmpp.packet.Packet;
import org.xmpp.resultsetmanagement.ResultSet;

public abstract class AbstractMessageProcessor implements PacketProcessor<Message> {

    protected Message message;
    protected String node;
    protected ChannelManager channelManager;
    protected Properties configuration;
    protected BlockingQueue<Packet> outQueue;
    
    public AbstractMessageProcessor(ChannelManager channelManager, Properties configuration, BlockingQueue<Packet> outQueue) {
        this.channelManager = channelManager;
        setConfiguration(configuration);
        this.outQueue = outQueue;
    }
    
    public void setConfiguration(Properties configuration) {
        this.configuration = configuration;
    }
    
    public abstract void process(Message packet) throws Exception;
    
    void sendLocalNotifications(NotificationScheme scheme) throws Exception {
        sendLocalNotifications(scheme, null);    
    }
    
    void sendLocalNotifications(NotificationScheme scheme, JID user) throws Exception {
        ResultSet<NodeMembership> members = channelManager
                .getNodeMemberships(node);
        message.setFrom(new JID(configuration
                .getProperty(Configuration.CONFIGURATION_SERVER_CHANNELS_DOMAIN)));
        
        for (NodeMembership member : members) {
            if (false == Configuration.getInstance().isLocalJID(member.getUser())) {
                continue;
            }
            if (scheme.equals(NotificationScheme.validSubscribers) && !userIsValidSubscriber(member)) {
                continue;
            } 
            if (scheme.equals(NotificationScheme.ownerOrModerator) && !userIsOwnerOrModerator(member)) {
                continue;
            }

            message.setTo(member.getUser());
            outQueue.put(message.createCopy());
        }
        
        if (null != user) {
            message.setTo(user.toBareJID());
            outQueue.put(message.createCopy());
        }
    }

    private boolean userIsOwnerOrModerator(NodeMembership member) {
        if (false == Subscriptions.subscribed.equals(member.getSubscription())) {
            return false;
        }
        if (false == member.getAffiliation().canAuthorize()) {
            return false;
        }
        return true;
    }

    private boolean userIsValidSubscriber(NodeMembership member) {
        if (false == Subscriptions.subscribed.equals(member.getSubscription())) {
            return false;
        }
        if (Affiliations.outcast.equals(member.getAffiliation())) {
            return false;
        }
        return true;
    }
}
