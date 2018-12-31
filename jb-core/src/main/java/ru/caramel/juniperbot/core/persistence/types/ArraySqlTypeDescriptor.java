/*
 * This file is part of JuniperBotJ.
 *
 * JuniperBotJ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * JuniperBotJ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with JuniperBotJ. If not, see <http://www.gnu.org/licenses/>.
 */
package ru.caramel.juniperbot.core.persistence.types;

import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.sql.BasicBinder;
import org.hibernate.type.descriptor.sql.BasicExtractor;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;

import java.sql.*;

public class ArraySqlTypeDescriptor
        implements SqlTypeDescriptor {

    private static final long serialVersionUID = -2825316581273628400L;

    public static final ArraySqlTypeDescriptor INSTANCE =
            new ArraySqlTypeDescriptor();

    @Override
    public int getSqlType() {
        return Types.ARRAY;
    }

    @Override
    public boolean canBeRemapped() {
        return true;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <X> ValueBinder<X> getBinder(
            JavaTypeDescriptor<X> javaTypeDescriptor) {
        return new BasicBinder<X>(javaTypeDescriptor, this) {
            @Override
            protected void doBind(
                    PreparedStatement st,
                    X value,
                    int index,
                    WrapperOptions options
            ) throws SQLException {

                AbstractArrayTypeDescriptor<Object> abstractArrayTypeDescriptor =
                        (AbstractArrayTypeDescriptor<Object>)
                                javaTypeDescriptor;

                st.setArray(
                        index,
                        st.getConnection().createArrayOf(
                                abstractArrayTypeDescriptor.getSqlArrayType(),
                                abstractArrayTypeDescriptor.unwrap(
                                        value,
                                        Object[].class,
                                        options
                                )
                        )
                );
            }

            @Override
            protected void doBind(
                    CallableStatement st,
                    X value,
                    String name,
                    WrapperOptions options
            ) throws SQLException {
                throw new UnsupportedOperationException(
                        "Binding by name is not supported!"
                );
            }
        };
    }

    @Override
    public <X> ValueExtractor<X> getExtractor(
            final JavaTypeDescriptor<X> javaTypeDescriptor) {
        return new BasicExtractor<X>(javaTypeDescriptor, this) {
            @Override
            protected X doExtract(
                    ResultSet rs,
                    String name,
                    WrapperOptions options
            ) throws SQLException {
                return javaTypeDescriptor.wrap(
                        rs.getArray(name),
                        options
                );
            }

            @Override
            protected X doExtract(
                    CallableStatement statement,
                    int index,
                    WrapperOptions options
            ) throws SQLException {
                return javaTypeDescriptor.wrap(
                        statement.getArray(index),
                        options
                );
            }

            @Override
            protected X doExtract(
                    CallableStatement statement,
                    String name,
                    WrapperOptions options
            ) throws SQLException {
                return javaTypeDescriptor.wrap(
                        statement.getArray(name),
                        options
                );
            }
        };
    }
}