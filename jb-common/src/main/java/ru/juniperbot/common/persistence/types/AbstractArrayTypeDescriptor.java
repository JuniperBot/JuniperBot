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
package ru.juniperbot.common.persistence.types;

import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.AbstractTypeDescriptor;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.java.MutableMutabilityPlan;
import org.hibernate.usertype.DynamicParameterizedType;
import ru.juniperbot.common.utils.ArrayUtil;

import java.sql.Array;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Properties;

public abstract class AbstractArrayTypeDescriptor<T>
        extends AbstractTypeDescriptor<T>
        implements DynamicParameterizedType {

    private static final long serialVersionUID = 8996147933030468081L;

    private Class<T> arrayObjectClass;

    @Override
    @SuppressWarnings("unchecked")
    public void setParameterValues(Properties parameters) {
        arrayObjectClass = ((ParameterType) parameters
                .get(PARAMETER_TYPE))
                .getReturnedClass();

    }

    @SuppressWarnings("unchecked")
    public AbstractArrayTypeDescriptor(Class<T> arrayObjectClass) {
        super(
                arrayObjectClass,
                (MutabilityPlan<T>) new MutableMutabilityPlan<Object>() {
                    private static final long serialVersionUID = 5441650086018349126L;

                    @Override
                    protected T deepCopyNotNull(Object value) {
                        return ArrayUtil.deepCopy(value);
                    }
                }
        );
        this.arrayObjectClass = arrayObjectClass;
    }

    @Override
    public boolean areEqual(Object one, Object another) {
        if (one == another) {
            return true;
        }
        if (one == null || another == null) {
            return false;
        }
        return ArrayUtil.isEquals(one, another);
    }

    @Override
    public String toString(Object value) {
        return Arrays.deepToString((Object[]) value);
    }

    @Override
    public T fromString(String string) {
        return ArrayUtil.fromString(
                string,
                arrayObjectClass
        );
    }

    @SuppressWarnings({"unchecked"})
    @Override
    public <X> X unwrap(
            T value,
            Class<X> type,
            WrapperOptions options
    ) {
        return (X) ArrayUtil.wrapArray(value);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <X> T wrap(
            X value,
            WrapperOptions options
    ) {
        if (value instanceof Array) {
            Array array = (Array) value;
            try {
                return ArrayUtil.unwrapArray(
                        (Object[]) array.getArray(),
                        arrayObjectClass
                );
            } catch (SQLException e) {
                throw new IllegalArgumentException(e);
            }
        }
        return (T) value;
    }

    protected abstract String getSqlArrayType();
}