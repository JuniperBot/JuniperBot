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
package ru.caramel.juniperbot.core.common.persistence.support;

import org.hibernate.QueryException;
import org.hibernate.dialect.function.SQLFunction;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.type.FloatType;
import org.hibernate.type.Type;

import java.util.List;

public class PostgreSQLToRankCDFunction implements SQLFunction {

    @Override
    public Type getReturnType(Type columnType, Mapping mapping)
            throws QueryException {
        return FloatType.INSTANCE;
    }

    @Override
    public String render(Type firstArgumentType, List arguments, SessionFactoryImplementor factory) throws QueryException {
        if (arguments == null || arguments.size() < 2) {
            throw new IllegalArgumentException("The function must be passed at least 2 arguments");
        }

        String fragment;
        String ftsConfig;
        String tsVector;
        String value;
        String normalization;
        if (arguments.size() == 4) {
            ftsConfig = (String) arguments.get(0);
            tsVector = (String) arguments.get(1);
            value = (String) arguments.get(2);
            normalization = (String) arguments.get(3);
            fragment = String.format("ts_rank_cd(%s, plainto_tsquery(%s, %s), %s)", tsVector, ftsConfig, value, normalization);
        } else {
            if (arguments.size() == 3) {
                ftsConfig = (String) arguments.get(0);
                tsVector = (String) arguments.get(1);
                value = (String) arguments.get(2);
                fragment = String.format("ts_rank_cd(%s, plainto_tsquery(%s, %s))", tsVector, ftsConfig, value);
            } else {
                tsVector = (String) arguments.get(0);
                value = (String) arguments.get(1);
                fragment = String.format("ts_rank_cd(%s, plainto_tsquery(%s))", tsVector, value);
            }
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