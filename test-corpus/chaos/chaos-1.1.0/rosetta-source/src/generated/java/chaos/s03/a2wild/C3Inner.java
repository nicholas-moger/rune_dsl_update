package chaos.s03.a2wild;

import chaos.s03.a2wild.meta.C3InnerMeta;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.annotations.Accessor;
import com.rosetta.model.lib.annotations.AccessorType;
import com.rosetta.model.lib.annotations.RosettaAttribute;
import com.rosetta.model.lib.annotations.RosettaDataType;
import com.rosetta.model.lib.annotations.RuneAttribute;
import com.rosetta.model.lib.annotations.RuneChoiceType;
import com.rosetta.model.lib.annotations.RuneDataType;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.BuilderMerger;
import com.rosetta.model.lib.process.BuilderProcessor;
import com.rosetta.model.lib.process.Processor;
import java.util.Objects;

import static java.util.Optional.ofNullable;

/**
 * Inner choice - the findCommonAttribute surface.
 * @version 1.0.0
 */
@RosettaDataType(value="C3Inner", builder=C3Inner.C3InnerBuilderImpl.class, version="1.0.0")
@RuneDataType(value="C3Inner", model="chaos", builder=C3Inner.C3InnerBuilderImpl.class, version="1.0.0")
@RuneChoiceType
public interface C3Inner extends RosettaModelObject {

	C3InnerMeta metaData = new C3InnerMeta();

	/*********************** Getter Methods  ***********************/
	C3CashLeg getC3CashLeg();
	C3StockLeg getC3StockLeg();

	/*********************** Build Methods  ***********************/
	C3Inner build();
	
	C3Inner.C3InnerBuilder toBuilder();
	
	static C3Inner.C3InnerBuilder builder() {
		return new C3Inner.C3InnerBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C3Inner> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C3Inner> getType() {
		return C3Inner.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processRosetta(path.newSubPath("C3CashLeg"), processor, C3CashLeg.class, getC3CashLeg());
		processRosetta(path.newSubPath("C3StockLeg"), processor, C3StockLeg.class, getC3StockLeg());
	}
	

	/*********************** Builder Interface  ***********************/
	interface C3InnerBuilder extends C3Inner, RosettaModelObjectBuilder {
		C3CashLeg.C3CashLegBuilder getOrCreateC3CashLeg();
		@Override
		C3CashLeg.C3CashLegBuilder getC3CashLeg();
		C3StockLeg.C3StockLegBuilder getOrCreateC3StockLeg();
		@Override
		C3StockLeg.C3StockLegBuilder getC3StockLeg();
		C3Inner.C3InnerBuilder setC3CashLeg(C3CashLeg _C3CashLeg);
		C3Inner.C3InnerBuilder setC3StockLeg(C3StockLeg _C3StockLeg);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processRosetta(path.newSubPath("C3CashLeg"), processor, C3CashLeg.C3CashLegBuilder.class, getC3CashLeg());
			processRosetta(path.newSubPath("C3StockLeg"), processor, C3StockLeg.C3StockLegBuilder.class, getC3StockLeg());
		}
		

		C3Inner.C3InnerBuilder prune();
	}

	/*********************** Immutable Implementation of C3Inner  ***********************/
	class C3InnerImpl implements C3Inner {
		private final C3CashLeg c3CashLeg;
		private final C3StockLeg c3StockLeg;
		
		protected C3InnerImpl(C3Inner.C3InnerBuilder builder) {
			this.c3CashLeg = ofNullable(builder.getC3CashLeg()).map(f->f.build()).orElse(null);
			this.c3StockLeg = ofNullable(builder.getC3StockLeg()).map(f->f.build()).orElse(null);
		}
		
		@Override
		@RosettaAttribute("C3CashLeg")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("C3CashLeg")
		public C3CashLeg getC3CashLeg() {
			return c3CashLeg;
		}
		
		@Override
		@RosettaAttribute("C3StockLeg")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("C3StockLeg")
		public C3StockLeg getC3StockLeg() {
			return c3StockLeg;
		}
		
		@Override
		public C3Inner build() {
			return this;
		}
		
		@Override
		public C3Inner.C3InnerBuilder toBuilder() {
			C3Inner.C3InnerBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C3Inner.C3InnerBuilder builder) {
			ofNullable(getC3CashLeg()).ifPresent(builder::setC3CashLeg);
			ofNullable(getC3StockLeg()).ifPresent(builder::setC3StockLeg);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C3Inner _that = getType().cast(o);
		
			if (!Objects.equals(c3CashLeg, _that.getC3CashLeg())) return false;
			if (!Objects.equals(c3StockLeg, _that.getC3StockLeg())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (c3CashLeg != null ? c3CashLeg.hashCode() : 0);
			_result = 31 * _result + (c3StockLeg != null ? c3StockLeg.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C3Inner {" +
				"C3CashLeg=" + this.c3CashLeg + ", " +
				"C3StockLeg=" + this.c3StockLeg +
			'}';
		}
	}

	/*********************** Builder Implementation of C3Inner  ***********************/
	class C3InnerBuilderImpl implements C3Inner.C3InnerBuilder {
	
		protected C3CashLeg.C3CashLegBuilder c3CashLeg;
		protected C3StockLeg.C3StockLegBuilder c3StockLeg;
		
		@Override
		@RosettaAttribute("C3CashLeg")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("C3CashLeg")
		public C3CashLeg.C3CashLegBuilder getC3CashLeg() {
			return c3CashLeg;
		}
		
		@Override
		public C3CashLeg.C3CashLegBuilder getOrCreateC3CashLeg() {
			C3CashLeg.C3CashLegBuilder result;
			if (c3CashLeg!=null) {
				result = c3CashLeg;
			}
			else {
				result = c3CashLeg = C3CashLeg.builder();
			}
			
			return result;
		}
		
		@Override
		@RosettaAttribute("C3StockLeg")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("C3StockLeg")
		public C3StockLeg.C3StockLegBuilder getC3StockLeg() {
			return c3StockLeg;
		}
		
		@Override
		public C3StockLeg.C3StockLegBuilder getOrCreateC3StockLeg() {
			C3StockLeg.C3StockLegBuilder result;
			if (c3StockLeg!=null) {
				result = c3StockLeg;
			}
			else {
				result = c3StockLeg = C3StockLeg.builder();
			}
			
			return result;
		}
		
		@RosettaAttribute("C3CashLeg")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("C3CashLeg")
		@Override
		public C3Inner.C3InnerBuilder setC3CashLeg(C3CashLeg _c3CashLeg) {
			this.c3CashLeg = _c3CashLeg == null ? null : _c3CashLeg.toBuilder();
			return this;
		}
		
		@RosettaAttribute("C3StockLeg")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("C3StockLeg")
		@Override
		public C3Inner.C3InnerBuilder setC3StockLeg(C3StockLeg _c3StockLeg) {
			this.c3StockLeg = _c3StockLeg == null ? null : _c3StockLeg.toBuilder();
			return this;
		}
		
		@Override
		public C3Inner build() {
			return new C3Inner.C3InnerImpl(this);
		}
		
		@Override
		public C3Inner.C3InnerBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C3Inner.C3InnerBuilder prune() {
			if (c3CashLeg!=null && !c3CashLeg.prune().hasData()) c3CashLeg = null;
			if (c3StockLeg!=null && !c3StockLeg.prune().hasData()) c3StockLeg = null;
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getC3CashLeg()!=null && getC3CashLeg().hasData()) return true;
			if (getC3StockLeg()!=null && getC3StockLeg().hasData()) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C3Inner.C3InnerBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C3Inner.C3InnerBuilder o = (C3Inner.C3InnerBuilder) other;
			
			merger.mergeRosetta(getC3CashLeg(), o.getC3CashLeg(), this::setC3CashLeg);
			merger.mergeRosetta(getC3StockLeg(), o.getC3StockLeg(), this::setC3StockLeg);
			
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C3Inner _that = getType().cast(o);
		
			if (!Objects.equals(c3CashLeg, _that.getC3CashLeg())) return false;
			if (!Objects.equals(c3StockLeg, _that.getC3StockLeg())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (c3CashLeg != null ? c3CashLeg.hashCode() : 0);
			_result = 31 * _result + (c3StockLeg != null ? c3StockLeg.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C3InnerBuilder {" +
				"C3CashLeg=" + this.c3CashLeg + ", " +
				"C3StockLeg=" + this.c3StockLeg +
			'}';
		}
	}
}
