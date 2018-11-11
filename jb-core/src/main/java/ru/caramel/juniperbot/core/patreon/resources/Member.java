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
package ru.caramel.juniperbot.core.patreon.resources;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.jasminb.jsonapi.annotations.Relationship;
import com.github.jasminb.jsonapi.annotations.Type;
import lombok.Getter;
import ru.caramel.juniperbot.core.patreon.resources.shared.BaseResource;
import ru.caramel.juniperbot.core.patreon.resources.shared.Field;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.stream.Collectors;

@Getter
@Type("member")
public class Member extends BaseResource {

    public enum MemberField implements Field {

        PATRON_STATUS("patron_status", true),
        IS_FOLLOWER("is_follower", true),
        FULL_NAME("full_name", true),
        EMAIL("email", true),
        PLEDGE_RELATIONSHIP_START("pledge_relationship_start", true),
        LIFETIME_SUPPORT_CENTS("lifetime_support_cents", true),
        CURRENTLY_ENTITLED_AMOUNT_CENTS("currently_entitled_amount_cents", true),
        LAST_CHARGE_DATE("last_charge_date", true),
        LAST_CHARGE_STATUS("last_charge_status", true),
        NOTE("note", true),
        WILL_PAY_AMOUNT_CENTS("will_pay_amount_cents", true),
        ;

        /**
         * The field's name from the API in JSON
         */
        @Getter
        public final String propertyName;

        /**
         * Whether the field is included by default
         */
        @Getter
        public final boolean isDefault;

        MemberField(String propertyName, boolean isDefault) {
            this.propertyName = propertyName;
            this.isDefault = isDefault;
        }

        public static Collection<MemberField> getDefaultFields() {
            return Arrays.stream(values()).filter(MemberField::isDefault).collect(Collectors.toList());
        }
    }

    private final String patronStatus;
    private final boolean follower;
    private final String fullName;
    private final String email;
    private final Date pledgeRelationshipStart;
    private final int lifetimeSupportCents;
    private final Integer currentlyEntitledAmountCents;
    private final Date lastChargeDate;
    private final String lastChargeStatus;
    private final String note;
    private final Integer willPayAmountCents;

    @Relationship("user")
    private final User user;

    @JsonCreator
    public Member(
            @JsonProperty("patron_status") String patronStatus,
            @JsonProperty("is_follower") boolean follower,
            @JsonProperty("full_name") String fullName,
            @JsonProperty("email") String email,
            @JsonProperty("pledge_relationship_start") Date pledgeRelationshipStart,
            @JsonProperty("lifetime_support_cents") int lifetimeSupportCents,
            @JsonProperty("currently_entitled_amount_cents") Integer currentlyEntitledAmountCents,
            @JsonProperty("last_charge_date") Date lastChargeDate,
            @JsonProperty("last_charge_status") String lastChargeStatus,
            @JsonProperty("note") String note,
            @JsonProperty("will_pay_amount_cents") Integer willPayAmountCents,
            @JsonProperty("user") User user
    ) {
        this.fullName = fullName;
        this.follower = follower;
        this.patronStatus = patronStatus;
        this.email = email;
        this.pledgeRelationshipStart = pledgeRelationshipStart;
        this.lifetimeSupportCents = lifetimeSupportCents;
        this.currentlyEntitledAmountCents = currentlyEntitledAmountCents;
        this.lastChargeDate = lastChargeDate;
        this.lastChargeStatus = lastChargeStatus;
        this.note = note;
        this.willPayAmountCents = willPayAmountCents;
        this.user = user;
    }

    public boolean isActiveAndPaid() {
        return "active_patron".equalsIgnoreCase(patronStatus) && "paid".equalsIgnoreCase(lastChargeStatus);
    }
}
