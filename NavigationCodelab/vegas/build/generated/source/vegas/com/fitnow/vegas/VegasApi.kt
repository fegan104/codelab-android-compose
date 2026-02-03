package com.fitnow.vegas

import kotlin.Boolean
import kotlin.Int
import kotlin.String
import kotlin.collections.Set

public sealed interface QuerySource {
  public object User : QuerySource

  public data class PromotionHistory(
    public val historyType: String,
    public val id: String,
  ) : QuerySource

  public object Configuration : QuerySource
}

public sealed interface Key<S : QuerySource, out T> {
  public fun resolve(context: DataContext, source: S): T?

  public interface IntKey<S : QuerySource> : Key<S, Int>

  public interface StringKey<S : QuerySource> : Key<S, String>

  public interface BoolKey<S : QuerySource> : Key<S, Boolean>

  public interface StringSetKey<S : QuerySource> : Key<S, Set<String>>
}

public interface DataContext {
  public fun fetchUserStringSet(source: QuerySource.User, key: UserKeys.UserStringSetKey): Set<String>?

  public fun fetchUserInt(source: QuerySource.User, key: UserKeys.UserIntKey): Int?

  public fun fetchUserBool(source: QuerySource.User, key: UserKeys.UserBoolKey): Boolean?

  public fun fetchPromotionHistoryInt(source: QuerySource.PromotionHistory, key: PromotionHistoryKeys.PromotionHistoryIntKey): Int?

  public fun fetchConfigurationBool(source: QuerySource.Configuration, key: ConfigurationKeys.ConfigurationBoolKey): Boolean?
}

public object UserKeys {
  public object Target : UserStringSetKey()

  public sealed class UserStringSetKey : Key.StringSetKey<QuerySource.User> {
    override fun resolve(context: DataContext, source: QuerySource.User): Set<String>? = context.fetchUserStringSet(source, this)
  }

  public object Day : UserIntKey()

  public object DaysSinceAccountCreated : UserIntKey()

  public sealed class UserIntKey : Key.IntKey<QuerySource.User> {
    override fun resolve(context: DataContext, source: QuerySource.User): Int? = context.fetchUserInt(source, this)
  }

  public object TrialState : UserBoolKey()

  public sealed class UserBoolKey : Key.BoolKey<QuerySource.User> {
    override fun resolve(context: DataContext, source: QuerySource.User): Boolean? = context.fetchUserBool(source, this)
  }
}

public object PromotionHistoryKeys {
  public object TimesShown : PromotionHistoryIntKey()

  public object DaysSinceLastShown : PromotionHistoryIntKey()

  public sealed class PromotionHistoryIntKey : Key.IntKey<QuerySource.PromotionHistory> {
    override fun resolve(context: DataContext, source: QuerySource.PromotionHistory): Int? = context.fetchPromotionHistoryInt(source, this)
  }
}

public object ConfigurationKeys {
  public object AndroidPremiumTimerTest : ConfigurationBoolKey()

  public sealed class ConfigurationBoolKey : Key.BoolKey<QuerySource.Configuration> {
    override fun resolve(context: DataContext, source: QuerySource.Configuration): Boolean? = context.fetchConfigurationBool(source, this)
  }
}
