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
package ru.juniperbot.common.persistence.support;

import org.hibernate.QueryException;
import org.hibernate.dialect.function.SQLFunction;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.type.BooleanType;
import org.hibernate.type.Type;

import java.util.List;

public class PostgreSQLPlainToTSQueryFunction implements SQLFunction {
    @Override
    public Type getReturnType(Type columnType, Mapping mapping)
            throws QueryException {
        return BooleanType.INSTANCE;
    }

    @Override
    public String render(Type firstArgumentType, List arguments, SessionFactoryImplementor factory) throws QueryException {
        if (arguments == null || arguments.size() < 2) {
            throw new IllegalArgumentException("The function must be passed 2 arguments");
        }

        String fragment;
        String ftsConfig;
        String field;
        String value;
        if (arguments.size() == 3) {
            ftsConfig = (String) arguments.get(0);
            field = (String) arguments.get(1);
            value = (String) arguments.get(2);
            fragment = field + " @@ plainto_tsquery(" + ftsConfig + ", " + value + ")";
        } else {
            field = (String) arguments.get(0);
            value = (String) arguments.get(1);
            fragment = field + " @@ plainto_tsquery(" + value + ")";
        }
        return fragment;
    }

    @Override
    public boolean hasArguments() {
        return true;
    }

    @Override
    public boolean hasParenthesesIfNoArguments() {
        return false;
    }
}