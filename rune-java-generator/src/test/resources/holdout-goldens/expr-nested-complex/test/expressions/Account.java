package test.expressions;

import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.annotations.Accessor;
import com.rosetta.model.lib.annotations.AccessorType;
import com.rosetta.model.lib.annotations.Required;
import com.rosetta.model.lib.annotations.RosettaAttribute;
import com.rosetta.model.lib.annotations.RosettaDataType;
import com.rosetta.model.lib.annotations.RuneAttribute;
import com.rosetta.model.lib.annotations.RuneDataType;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.BuilderMerger;
import com.rosetta.model.lib.process.BuilderProcessor;
import com.rosetta.model.lib.process.Processor;
import java.math.BigDecimal;
import java.util.Objects;
import test.expressions.meta.AccountMeta;

import static java.util.Optional.ofNullable;

/**
 * @version 0.0.0
 */
@RosettaDataType(value="Account", builder=Account.AccountBuilderImpl.class, version="0.0.0")
@RuneDataType(value="Account", model="test", builder=Account.AccountBuilderImpl.class, version="0.0.0")
public interface Account extends RosettaModelObject {

	AccountMeta metaData = new AccountMeta();

	/*********************** Getter Methods  ***********************/
	BigDecimal getBalance();
	Boolean getActive();

	/*********************** Build Methods  ***********************/
	Account build();
	
	Account.AccountBuilder toBuilder();
	
	static Account.AccountBuilder builder() {
		return new Account.AccountBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends Account> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends Account> getType() {
		return Account.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("balance"), BigDecimal.class, getBalance(), this);
		processor.processBasic(path.newSubPath("active"), Boolean.class, getActive(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface AccountBuilder extends Account, RosettaModelObjectBuilder {
		Account.AccountBuilder setBalance(BigDecimal balance);
		Account.AccountBuilder setActive(Boolean active);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("balance"), BigDecimal.class, getBalance(), this);
			processor.processBasic(path.newSubPath("active"), Boolean.class, getActive(), this);
		}
		

		Account.AccountBuilder prune();
	}

	/*********************** Immutable Implementation of Account  ***********************/
	class AccountImpl implements Account {
		private final BigDecimal balance;
		private final Boolean active;
		
		protected AccountImpl(Account.AccountBuilder builder) {
			this.balance = builder.getBalance();
			this.active = builder.getActive();
		}
		
		@Override
		@RosettaAttribute("balance")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("balance")
		public BigDecimal getBalance() {
			return balance;
		}
		
		@Override
		@RosettaAttribute("active")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("active")
		public Boolean getActive() {
			return active;
		}
		
		@Override
		public Account build() {
			return this;
		}
		
		@Override
		public Account.AccountBuilder toBuilder() {
			Account.AccountBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(Account.AccountBuilder builder) {
			ofNullable(getBalance()).ifPresent(builder::setBalance);
			ofNullable(getActive()).ifPresent(builder::setActive);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Account _that = getType().cast(o);
		
			if (!Objects.equals(balance, _that.getBalance())) return false;
			if (!Objects.equals(active, _that.getActive())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (balance != null ? balance.hashCode() : 0);
			_result = 31 * _result + (active != null ? active.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "Account {" +
				"balance=" + this.balance + ", " +
				"active=" + this.active +
			'}';
		}
	}

	/*********************** Builder Implementation of Account  ***********************/
	class AccountBuilderImpl implements Account.AccountBuilder {
	
		protected BigDecimal balance;
		protected Boolean active;
		
		@Override
		@RosettaAttribute("balance")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("balance")
		public BigDecimal getBalance() {
			return balance;
		}
		
		@Override
		@RosettaAttribute("active")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("active")
		public Boolean getActive() {
			return active;
		}
		
		@RosettaAttribute("balance")
		@Accessor(AccessorType.SETTER)
		@Required
		@RuneAttribute("balance")
		@Override
		public Account.AccountBuilder setBalance(BigDecimal _balance) {
			this.balance = _balance == null ? null : _balance;
			return this;
		}
		
		@RosettaAttribute("active")
		@Accessor(AccessorType.SETTER)
		@Required
		@RuneAttribute("active")
		@Override
		public Account.AccountBuilder setActive(Boolean _active) {
			this.active = _active == null ? null : _active;
			return this;
		}
		
		@Override
		public Account build() {
			return new Account.AccountImpl(this);
		}
		
		@Override
		public Account.AccountBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public Account.AccountBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getBalance()!=null) return true;
			if (getActive()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public Account.AccountBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			Account.AccountBuilder o = (Account.AccountBuilder) other;
			
			
			merger.mergeBasic(getBalance(), o.getBalance(), this::setBalance);
			merger.mergeBasic(getActive(), o.getActive(), this::setActive);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Account _that = getType().cast(o);
		
			if (!Objects.equals(balance, _that.getBalance())) return false;
			if (!Objects.equals(active, _that.getActive())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (balance != null ? balance.hashCode() : 0);
			_result = 31 * _result + (active != null ? active.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "AccountBuilder {" +
				"balance=" + this.balance + ", " +
				"active=" + this.active +
			'}';
		}
	}
}
