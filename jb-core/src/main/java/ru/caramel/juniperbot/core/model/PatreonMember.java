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
package ru.caramel.juniperbot.core.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.jasminb.jsonapi.annotations.Relationship;
import com.github.jasminb.jsonapi.annotations.Type;
import com.patreon.resources.Campaign;
import com.patreon.resources.User;
import com.patreon.resources.shared.BaseResource;
import lombok.Getter;

import java.util.Date;

@Getter
@Type("member")
public class PatreonMember extends BaseResource {

    private String fullName;
    private boolean follower;
    private Date lastChargeDate;
    private String lastChargeStatus;
    private int lifetimeSupportCents;
    private String patronStatus;
    private Integer pledgeAmountCents;
    private Integer currentlyEntitledAmountCents;
    private Integer pledgeCapAmountCents;
    private Date pledgeRelationshipStart;

    @Relationship("campaign")
    private Campaign campaign;

    @Relationship("user")
    private User user;

    @JsonCreator
    public PatreonMember(
            @JsonProperty("full_name") String fullName,
            @JsonProperty("is_follower") boolean follower,
            @JsonProperty("last_charge_date") Date lastChargeDate,
            @JsonProperty("last_charge_status") String lastChargeStatus,
            @JsonProperty("lifetime_support_cents") int lifetimeSupportCents,
            @JsonProperty("patron_status") String patronStatus,
            @JsonProperty("pledge_amount_cents") Integer pledgeAmountCents,
            @JsonProperty("currently_entitled_amount_cents") Integer currentlyEntitledAmountCents,
            @JsonProperty("pledge_cap_amount_cents") Integer pledgeCapAmountCents,
            @JsonProperty("pledge_relationship_start") Date pledgeRelationshipStart,
            @JsonProperty("campaign") Campaign campaign,
            @JsonProperty("user") User user
    ) {
        this.fullName = fullName;
        this.follower = follower;
        this.lastChargeDate = lastChargeDate;
        this.lastChargeStatus = lastChargeStatus;
        this.lifetimeSupportCents = lifetimeSupportCents;
        this.patronStatus = patronStatus;
        this.pledgeAmountCents = pledgeAmountCents;
        this.currentlyEntitledAmountCents = currentlyEntitledAmountCents;
        this.pledgeCapAmountCents = pledgeCapAmountCents;
        this.pledgeRelationshipStart = pledgeRelationshipStart;
        this.campaign = campaign;
        this.user = user;
    }
}
