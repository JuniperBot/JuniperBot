/*
 * This file is part of JuniperBot.
 *
 * JuniperBot is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * JuniperBot is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with JuniperBot. If not, see <http://www.gnu.org/licenses/>.
 */
package ru.juniperbot.common.persistence.types.json;

import com.fasterxml.jackson.databind.JsonNode;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.sql.BasicBinder;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class JsonBinarySqlTypeDescriptor
        extends AbstractJsonSqlTypeDescriptor {

    private static final long serialVersionUID = 925608129281277893L;

    public static final JsonBinarySqlTypeDescriptor INSTANCE =
            new JsonBinarySqlTypeDescriptor();

    @Override
    public <X> ValueBinder<X> getBinder(
            final JavaTypeDescriptor<X> javaTypeDescriptor) {
        return new BasicBinder<X>(javaTypeDescriptor, this) {
            @Override
            protected void doBind(
                    PreparedStatement st,
                    X value,
                    int index,
                    WrapperOptions options) throws SQLException {
                st.setObject(index,
                        javaTypeDescriptor.unwrap(
                                value, JsonNode.class, options), getSqlType()
                );
            }

            @Override
            protected void doBind(
                    CallableStatement st,
                    X value,
                    String name,
                    WrapperOptions options)
                    throws SQLException {
                st.setObject(name,
                        javaTypeDescriptor.unwrap(
                                value, JsonNode.class, options), getSqlType()
                );
            }
        };
    }
}
