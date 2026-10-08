package holdout.voidexistsclean;

import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.annotations.Accessor;
import com.rosetta.model.lib.annotations.AccessorType;
import com.rosetta.model.lib.annotations.RosettaAttribute;
import com.rosetta.model.lib.annotations.RosettaDataType;
import com.rosetta.model.lib.annotations.RuneAttribute;
import com.rosetta.model.lib.annotations.RuneDataType;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.BuilderMerger;
import com.rosetta.model.lib.process.BuilderProcessor;
import com.rosetta.model.lib.process.Processor;
import holdout.voidexistsclean.meta.PairMeta;
import java.util.Objects;

import static java.util.Optional.ofNullable;

/**
 * An output carrying an existence result beside a plain string leaf.
 * @version 0.0.0
 */
@RosettaDataType(value="Pair", builder=Pair.PairBuilderImpl.class, version="0.0.0")
@RuneDataType(value="Pair", model="holdout", builder=Pair.PairBuilderImpl.class, version="0.0.0")
public interface Pair extends RosettaModelObject {

	PairMeta metaData = new PairMeta();

	/*********************** Getter Methods  ***********************/
	Boolean getPresent();
	String getS();

	/*********************** Build Methods  ***********************/
	Pair build();
	
	Pair.PairBuilder toBuilder();
	
	static Pair.PairBuilder builder() {
		return new Pair.PairBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends Pair> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends Pair> getType() {
		return Pair.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("present"), Boolean.class, getPresent(), this);
		processor.processBasic(path.newSubPath("s"), String.class, getS(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface PairBuilder extends Pair, RosettaModelObjectBuilder {
		Pair.PairBuilder setPresent(Boolean present);
		Pair.PairBuilder setS(String s);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("present"), Boolean.class, getPresent(), this);
			processor.processBasic(path.newSubPath("s"), String.class, getS(), this);
		}
		

		Pair.PairBuilder prune();
	}

	/*********************** Immutable Implementation of Pair  ***********************/
	class PairImpl implements Pair {
		private final Boolean present;
		private final String s;
		
		protected PairImpl(Pair.PairBuilder builder) {
			this.present = builder.getPresent();
			this.s = builder.getS();
		}
		
		@Override
		@RosettaAttribute("present")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("present")
		public Boolean getPresent() {
			return present;
		}
		
		@Override
		@RosettaAttribute("s")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("s")
		public String getS() {
			return s;
		}
		
		@Override
		public Pair build() {
			return this;
		}
		
		@Override
		public Pair.PairBuilder toBuilder() {
			Pair.PairBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(Pair.PairBuilder builder) {
			ofNullable(getPresent()).ifPresent(builder::setPresent);
			ofNullable(getS()).ifPresent(builder::setS);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Pair _that = getType().cast(o);
		
			if (!Objects.equals(present, _that.getPresent())) return false;
			if (!Objects.equals(s, _that.getS())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (present != null ? present.hashCode() : 0);
			_result = 31 * _result + (s != null ? s.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "Pair {" +
				"present=" + this.present + ", " +
				"s=" + this.s +
			'}';
		}
	}

	/*********************** Builder Implementation of Pair  ***********************/
	class PairBuilderImpl implements Pair.PairBuilder {
	
		protected Boolean present;
		protected String s;
		
		@Override
		@RosettaAttribute("present")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("present")
		public Boolean getPresent() {
			return present;
		}
		
		@Override
		@RosettaAttribute("s")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("s")
		public String getS() {
			return s;
		}
		
		@RosettaAttribute("present")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("present")
		@Override
		public Pair.PairBuilder setPresent(Boolean _present) {
			this.present = _present == null ? null : _present;
			return this;
		}
		
		@RosettaAttribute("s")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("s")
		@Override
		public Pair.PairBuilder setS(String _s) {
			this.s = _s == null ? null : _s;
			return this;
		}
		
		@Override
		public Pair build() {
			return new Pair.PairImpl(this);
		}
		
		@Override
		public Pair.PairBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public Pair.PairBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getPresent()!=null) return true;
			if (getS()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public Pair.PairBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			Pair.PairBuilder o = (Pair.PairBuilder) other;
			
			
			merger.mergeBasic(getPresent(), o.getPresent(), this::setPresent);
			merger.mergeBasic(getS(), o.getS(), this::setS);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Pair _that = getType().cast(o);
		
			if (!Objects.equals(present, _that.getPresent())) return false;
			if (!Objects.equals(s, _that.getS())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (present != null ? present.hashCode() : 0);
			_result = 31 * _result + (s != null ? s.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "PairBuilder {" +
				"present=" + this.present + ", " +
				"s=" + this.s +
			'}';
		}
	}
}
