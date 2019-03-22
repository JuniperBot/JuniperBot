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
package ru.caramel.juniperbot.core.metrics.persistence;

import com.codahale.metrics.Metric;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.Type;
import ru.caramel.juniperbot.core.common.persistence.base.BaseEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.Map;

@Entity
@Table(name = "metric")
@ToString
@Getter
@Setter
public class StoredMetric extends BaseEntity {

    private static final long serialVersionUID = 1086198114157452394L;

    @Column
    private String name;

    @Column
    private Class<? extends Metric> type;

    @Column
    private Long count;

    @Type(type = "jsonb")
    @Column(columnDefinition = "json")
    private Map<String, Object> data;
}
