package ru.caramel.juniperbot.core.support.jmx;

import org.springframework.jmx.export.naming.MetadataNamingStrategy;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

public class JbMetadataNamingStrategy extends MetadataNamingStrategy {

    /**
     * Overrides Spring's naming method and replaced it with our local one.
     */
    @Override
    public ObjectName getObjectName(Object managedBean, String beanKey) throws MalformedObjectNameException {
        if (beanKey == null) {
            beanKey = managedBean.getClass().getName();
        }
        ObjectName objectName = super.getObjectName(managedBean, beanKey);
        if (managedBean instanceof JmxNamedResource) {
            objectName = buildObjectName((JmxNamedResource) managedBean, objectName.getDomain());
        }
        return objectName;
    }

    /**
     * Construct our object name by calling the methods in {@link JmxNamedResource}.
     */
    private ObjectName buildObjectName(JmxNamedResource namedObject, String domainName)
            throws MalformedObjectNameException {

        String[] typeNames = namedObject.getJmxPath();
        if (typeNames == null) {
            throw new MalformedObjectNameException("getJmxPath() is returning null for object " + namedObject);
        }

        StringBuilder nameBuilder = new StringBuilder();
        nameBuilder.append(domainName);
        nameBuilder.append(':');

        /*
         * Ok. This is a HACK. It seems like something in the JMX mbean naming process actually sorts the names
         * lexicographically. The stuff before the '=' character seems to be ignored anyway but it does look ugly.
         */
        boolean needComma = false;
        int typeNameC = 0;
        for (String typeName : typeNames) {
            if (needComma) {
                nameBuilder.append(',');
            }
            // this will come out as 00=partition
            nameBuilder.append(String.format("%02d", typeNameC));
            typeNameC++;
            nameBuilder.append('=');
            nameBuilder.append(typeName);
            needComma = true;
        }
        if (needComma) {
            nameBuilder.append(',');
        }
        nameBuilder.append("name=");
        nameBuilder.append(namedObject.getJmxName());
        return ObjectName.getInstance(nameBuilder.toString());
    }
}
