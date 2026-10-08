package test.aliasheader;

import com.google.common.collect.ImmutableList;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.annotations.Accessor;
import com.rosetta.model.lib.annotations.AccessorType;
import com.rosetta.model.lib.annotations.Multi;
import com.rosetta.model.lib.annotations.RosettaAttribute;
import com.rosetta.model.lib.annotations.RosettaDataType;
import com.rosetta.model.lib.annotations.RuneAttribute;
import com.rosetta.model.lib.annotations.RuneDataType;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.BuilderMerger;
import com.rosetta.model.lib.process.BuilderProcessor;
import com.rosetta.model.lib.process.Processor;
import com.rosetta.util.ListEquals;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import test.aliasheader.meta.PayLoadMeta;

import static java.util.Optional.ofNullable;

/**
 * the validator&#39;s data class shares its simple name with the condition class it wires.
 * @version 0.0.0.test
 */
@RosettaDataType(value="PayLoad", builder=PayLoad.PayLoadBuilderImpl.class, version="0.0.0.test")
@RuneDataType(value="PayLoad", model="test", builder=PayLoad.PayLoadBuilderImpl.class, version="0.0.0.test")
public interface PayLoad extends RosettaModelObject {

	PayLoadMeta metaData = new PayLoadMeta();

	/*********************** Getter Methods  ***********************/
	Integer getPay();
	List<Integer> getPays();

	/*********************** Build Methods  ***********************/
	PayLoad build();
	
	PayLoad.PayLoadBuilder toBuilder();
	
	static PayLoad.PayLoadBuilder builder() {
		return new PayLoad.PayLoadBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends PayLoad> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends PayLoad> getType() {
		return PayLoad.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("pay"), Integer.class, getPay(), this);
		processor.processBasic(path.newSubPath("pays"), Integer.class, getPays(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface PayLoadBuilder extends PayLoad, RosettaModelObjectBuilder {
		PayLoad.PayLoadBuilder setPay(Integer pay);
		PayLoad.PayLoadBuilder addPays(Integer pays);
		PayLoad.PayLoadBuilder addPays(Integer pays, int idx);
		PayLoad.PayLoadBuilder addPays(List<Integer> pays);
		PayLoad.PayLoadBuilder setPays(List<Integer> pays);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("pay"), Integer.class, getPay(), this);
			processor.processBasic(path.newSubPath("pays"), Integer.class, getPays(), this);
		}
		

		PayLoad.PayLoadBuilder prune();
	}

	/*********************** Immutable Implementation of PayLoad  ***********************/
	class PayLoadImpl implements PayLoad {
		private final Integer pay;
		private final List<Integer> pays;
		
		protected PayLoadImpl(PayLoad.PayLoadBuilder builder) {
			this.pay = builder.getPay();
			this.pays = ofNullable(builder.getPays()).filter(_l->!_l.isEmpty()).map(ImmutableList::copyOf).orElse(null);
		}
		
		@Override
		@RosettaAttribute("pay")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("pay")
		public Integer getPay() {
			return pay;
		}
		
		@Override
		@RosettaAttribute("pays")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("pays")
		public List<Integer> getPays() {
			return pays;
		}
		
		@Override
		public PayLoad build() {
			return this;
		}
		
		@Override
		public PayLoad.PayLoadBuilder toBuilder() {
			PayLoad.PayLoadBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(PayLoad.PayLoadBuilder builder) {
			ofNullable(getPay()).ifPresent(builder::setPay);
			ofNullable(getPays()).ifPresent(builder::setPays);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			PayLoad _that = getType().cast(o);
		
			if (!Objects.equals(pay, _that.getPay())) return false;
			if (!ListEquals.listEquals(pays, _that.getPays())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (pay != null ? pay.hashCode() : 0);
			_result = 31 * _result + (pays != null ? pays.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "PayLoad {" +
				"pay=" + this.pay + ", " +
				"pays=" + this.pays +
			'}';
		}
	}

	/*********************** Builder Implementation of PayLoad  ***********************/
	class PayLoadBuilderImpl implements PayLoad.PayLoadBuilder {
	
		protected Integer pay;
		protected List<Integer> pays = new ArrayList<>();
		
		@Override
		@RosettaAttribute("pay")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("pay")
		public Integer getPay() {
			return pay;
		}
		
		@Override
		@RosettaAttribute("pays")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("pays")
		public List<Integer> getPays() {
			return pays;
		}
		
		@RosettaAttribute("pay")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("pay")
		@Override
		public PayLoad.PayLoadBuilder setPay(Integer _pay) {
			this.pay = _pay == null ? null : _pay;
			return this;
		}
		
		@RosettaAttribute("pays")
		@Accessor(AccessorType.ADDER)
		@Multi
		@RuneAttribute("pays")
		@Override
		public PayLoad.PayLoadBuilder addPays(Integer _pays) {
			if (_pays != null) {
				this.pays.add(_pays);
			}
			return this;
		}
		
		@Override
		public PayLoad.PayLoadBuilder addPays(Integer _pays, int idx) {
			getIndex(this.pays, idx, () -> _pays);
			return this;
		}
		
		@Override
		public PayLoad.PayLoadBuilder addPays(List<Integer> payss) {
			if (payss != null) {
				for (final Integer toAdd : payss) {
					this.pays.add(toAdd);
				}
			}
			return this;
		}
		
		@RosettaAttribute("pays")
		@Accessor(AccessorType.SETTER)
		@Multi
		@RuneAttribute("pays")
		@Override
		public PayLoad.PayLoadBuilder setPays(List<Integer> payss) {
			if (payss == null) {
				this.pays = new ArrayList<>();
			} else {
				this.pays = payss.stream()
					.collect(Collectors.toCollection(()->new ArrayList<>()));
			}
			return this;
		}
		
		@Override
		public PayLoad build() {
			return new PayLoad.PayLoadImpl(this);
		}
		
		@Override
		public PayLoad.PayLoadBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public PayLoad.PayLoadBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getPay()!=null) return true;
			if (getPays()!=null && !getPays().isEmpty()) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public PayLoad.PayLoadBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			PayLoad.PayLoadBuilder o = (PayLoad.PayLoadBuilder) other;
			
			
			merger.mergeBasic(getPay(), o.getPay(), this::setPay);
			merger.mergeBasic(getPays(), o.getPays(), (Consumer<Integer>) this::addPays);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			PayLoad _that = getType().cast(o);
		
			if (!Objects.equals(pay, _that.getPay())) return false;
			if (!ListEquals.listEquals(pays, _that.getPays())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (pay != null ? pay.hashCode() : 0);
			_result = 31 * _result + (pays != null ? pays.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "PayLoadBuilder {" +
				"pay=" + this.pay + ", " +
				"pays=" + this.pays +
			'}';
		}
	}
}
