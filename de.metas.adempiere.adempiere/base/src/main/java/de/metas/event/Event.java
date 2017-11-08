package de.metas.event;

/*
 * #%L
 * de.metas.adempiere.adempiere.base
 * %%
 * Copyright (C) 2015 metas GmbH
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program. If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.util.Check;
import org.adempiere.util.GuavaCollectors;
import org.adempiere.util.lang.ITableRecordReference;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.Value;

/**
 * Event that can be sent/received on {@link IEventBus}.
 *
 * @author tsa
 *
 */
@JsonAutoDetect(fieldVisibility = Visibility.ANY, getterVisibility = Visibility.NONE, isGetterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE)
@Value
public final class Event
{
	public static final Builder builder()
	{
		return new Builder();
	}

	private static final String PROPERTY_Record = "record";
	public static final String PROPERTY_SuggestedWindowId = "suggestedWindowId";

	@JsonProperty("id")
	private final String id;

	@JsonProperty("summary")
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	private final String summary;

	@JsonProperty("detailPlain")
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	private final String detailPlain;

	@JsonProperty("detailADMessage")
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	private final String detailADMessage;

	@JsonProperty("senderId")
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	private final String senderId;

	@JsonProperty("recipientUserIds")
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	private final ImmutableSet<Integer> recipientUserIds;

	@JsonProperty("properties")
	@JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS)
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	private final ImmutableMap<String, Object> properties;

	//
	@JsonIgnore
	@Getter(AccessLevel.NONE)
	private final transient Set<String> receivedByEventBusIds = Sets.newConcurrentHashSet();

	private Event(final Builder builder)
	{
		if (builder.id != null)
		{
			id = builder.id;
		}
		else
		{
			id = UUID.randomUUID().toString();
		}
		summary = builder.summary;
		detailPlain = builder.getDetailPlain();
		detailADMessage = builder.getDetailADMessage();
		senderId = builder.senderId;
		recipientUserIds = ImmutableSet.copyOf(builder.recipientUserIds);
		properties = deepCopy(builder.getProperties());
	}

	@JsonCreator
	private Event(
			@JsonProperty("id") @NonNull final String id,
			@JsonProperty("summary") final String summary,
			@JsonProperty("detailPlain") final String detailPlain,
			@JsonProperty("detailADMessage") final String detailADMessage,
			@JsonProperty("senderId") final String senderId,
			@JsonProperty("recipientUserIds") final Set<Integer> recipientUserIds,
			@JsonProperty("properties") final Map<String, Object> properties)
	{
		this.id = id;
		this.summary = summary;
		this.detailPlain = detailPlain;
		this.detailADMessage = detailADMessage;
		this.senderId = senderId;
		this.recipientUserIds = recipientUserIds != null ? ImmutableSet.copyOf(recipientUserIds) : ImmutableSet.of();
		this.properties = deepCopy(properties);
	}

	private static final ImmutableMap<String, Object> deepCopy(final Map<String, Object> properties)
	{
		if (properties == null || properties.isEmpty())
		{
			return ImmutableMap.of();
		}

		return properties.entrySet()
				.stream()
				.filter(e -> e.getValue() != null) // skip nulls
				.collect(GuavaCollectors.toImmutableMap());
	}

	public boolean isLocalEvent()
	{
		return Objects.equals(EventBusConstants.getSenderId(), senderId);
	}

	public final boolean hasRecipient(final int userId)
	{
		if (isAllRecipients())
		{
			return true;
		}

		return recipientUserIds.contains(userId);
	}

	/**
	 * @return true if this event is for all users
	 */
	public boolean isAllRecipients()
	{
		// If no recipients were specified, consider that this event is for anybody
		return recipientUserIds.isEmpty();
	}

	/**
	 * 
	 * @param name
	 * @return tje propertiy with the given name or {@code null}
	 */
	public <T> T getProperty(final String name)
	{
		@SuppressWarnings("unchecked")
		final T value = (T)properties.get(name);
		return value;
	}

	/**
	 * @return record or null
	 * @see #getProperty(String)
	 * @see Builder#setRecord(ITableRecordReference)
	 */
	public ITableRecordReference getRecord()
	{
		final ITableRecordReference record = getProperty(PROPERTY_Record);
		return record;
	}

	/**
	 *
	 * @param eventBusId
	 * @return
	 *         <ul>
	 *         <li>true if event was successfully marked
	 *         <li>false if event was already received by given event bus ID
	 *         </ul>
	 *
	 */
	public final boolean markReceivedByEventBusId(final String eventBusId)
	{
		return receivedByEventBusIds.add(eventBusId);
	}

	/**
	 *
	 * @param eventBusId
	 * @return true if this event was received by a even bus with given ID.
	 */
	public final boolean wasReceivedByEventBusId(final String eventBusId)
	{
		return receivedByEventBusIds.contains(eventBusId);
	}

	//
	//
	// --------------------------------------------------------------------------------------------------------------------------------
	//
	//
	public static final class Builder
	{
		private String id;
		private String summary;
		private String detailPlain;
		private String detailADMessage;
		private String senderId = EventBusConstants.getSenderId();
		private final Set<Integer> recipientUserIds = new HashSet<>();
		private final Map<String, Object> properties = Maps.newLinkedHashMap();

		private Builder()
		{
			super();
		}

		public Event build()
		{
			return new Event(this);
		}

		public Builder setId(final String id)
		{
			Check.assumeNotEmpty(id, "id not empty");
			this.id = id;
			return this;
		}

		public Builder setSummary(final String summary)
		{
			this.summary = summary;
			return this;
		}

		public Builder setDetailPlain(final String detailPlain)
		{
			this.detailPlain = detailPlain;
			return this;
		}

		private String getDetailPlain()
		{
			return detailPlain;
		}

		/**
		 * Sets the detail AD_Message.
		 *
		 * NOTE: the message will not be translated, but it will be stored as it is and is expected to be translated when it's used (using the language of the recipient). If any parameters were
		 * provided, they will be added to event properties, using following convention:
		 * <ul>
		 * <li>first parameter will be put with name "0"
		 * <li>second parameter will be put with name "1"
		 * <li>etc
		 * </ul>
		 * Because the message parameters are stored as Event properties, please make sure the parameter type is supported (see <code>putProperty</code> methods).
		 *
		 * @param adMessage
		 * @param params AD_Message parameters
		 */
		public Builder setDetailADMessage(final String adMessage, final Object... params)
		{
			if (params != null && params.length > 0)
			{
				for (int i = 0; i < params.length; i++)
				{
					final String parameterName = String.valueOf(i);
					final Object parameterValue = params[i];
					putPropertyFromObject(parameterName, parameterValue);
				}
			}

			detailADMessage = adMessage;
			return this;
		}

		private String getDetailADMessage()
		{
			return detailADMessage;
		}

		public Builder setSenderId(final String senderId)
		{
			Check.assumeNotEmpty(senderId, "senderId not empty");
			this.senderId = senderId;
			return this;
		}

		public Builder addRecipient_User_ID(final int recipientUserId)
		{
			if (recipientUserId < 0)
			{
				return this;
			}

			recipientUserIds.add(recipientUserId);
			return this;
		}

		public Builder addRecipient_User_IDs(final Iterable<Integer> recipientUserIds)
		{
			if (recipientUserIds == null)
			{
				return this;
			}

			for (final Integer userId : recipientUserIds)
			{
				if (userId == null)
				{
					continue;
				}
				addRecipient_User_ID(userId);
			}

			return this;
		}

		private final Map<String, Object> getProperties()
		{
			return properties;
		}

		public Builder putProperty(final String name, final int value)
		{
			properties.put(name, value);
			return this;
		}

		public Builder putProperty(final String name, final long value)
		{
			properties.put(name, value);
			return this;
		}

		public Builder putProperty(final String name, final boolean value)
		{
			properties.put(name, value);
			return this;
		}

		public Builder putProperty(final String name, final String value)
		{
			properties.put(name, value);
			return this;
		}

		public Builder putProperty(final String name, final Date value)
		{
			properties.put(name, value);
			return this;
		}

		public Builder putProperty(final String name, final BigDecimal value)
		{
			properties.put(name, value);
			return this;
		}

		public Builder putProperty(final String name, final ITableRecordReference value)
		{
			properties.put(name, value);
			return this;
		}

		/**
		 * @see #putProperty(String, ITableRecordReference)
		 * @see Event#PROPERTY_Record
		 */
		public Builder setRecord(final ITableRecordReference record)
		{
			putProperty(Event.PROPERTY_Record, record);
			return this;
		}

		public final Builder putPropertyFromObject(final String name, final Object value)
		{
			if (value == null)
			{
				properties.remove(name);
				return this;
			}
			else if (value instanceof Integer)
			{
				return putProperty(name, (Integer)value);
			}
			else if (value instanceof Long)
			{
				return putProperty(name, (Long)value);
			}
			else if (value instanceof Double)
			{
				final Double doubleValue = (Double)value;
				final int intValue = doubleValue.intValue();
				if (doubleValue.doubleValue() == intValue)
				{
					return putProperty(name, intValue);
				}
				else
				{
					return putProperty(name, BigDecimal.valueOf(doubleValue));
				}
			}
			else if (value instanceof String)
			{
				return putProperty(name, (String)value);
			}
			else if (value instanceof Date)
			{
				return putProperty(name, (Date)value);
			}
			else if (value instanceof Boolean)
			{
				return putProperty(name, (Boolean)value);
			}
			else if (value instanceof ITableRecordReference)
			{
				return putProperty(name, (ITableRecordReference)value);
			}
			else if (value instanceof BigDecimal)
			{
				return putProperty(name, (BigDecimal)value);
			}
			else
			{
				throw new AdempiereException("Unknown value type " + name + " = " + value + " (type " + value.getClass() + ")");
			}
		}

		public Builder setSuggestedWindowId(int suggestedWindowId)
		{
			putProperty(PROPERTY_SuggestedWindowId, suggestedWindowId);
			return this;
		}

	}
}
