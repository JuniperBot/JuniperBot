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
package ru.caramel.juniperbot.persistence.types.json;

import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.sql.BasicExtractor;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

public abstract class AbstractJsonSqlTypeDescriptor
        implements SqlTypeDescriptor {

    private static final long serialVersionUID = -8941996480499577393L;

    @Override
    public int getSqlType() {
        return Types.OTHER;
    }

    @Override
    public boolean canBeRemapped() {
        return true;
    }

    @Override
    public <X> ValueExtractor<X> getExtractor(
            final JavaTypeDescriptor<X> javaTypeDescriptor) {
        return new BasicExtractor<X>(javaTypeDescriptor, this) {
            @Override
            protected X doExtract(
                    ResultSet rs,
                    String name,
                    WrapperOptions options) throws SQLException {
                return javaTypeDescriptor.wrap(
                        rs.getObject(name), options
                );
            }

            @Override
            protected X doExtract(
                    CallableStatement statement,
                    int index,
                    WrapperOptions options) throws SQLException {
                return javaTypeDescriptor.wrap(
                        statement.getObject(index), options
                );
            }

            @Override
            protected X doExtract(
                    CallableStatement statement,
                    String name,
                    WrapperOptions options) throws SQLException {
                return javaTypeDescriptor.wrap(
                        statement.getObject(name), options
                );
            }
        };
    }

}