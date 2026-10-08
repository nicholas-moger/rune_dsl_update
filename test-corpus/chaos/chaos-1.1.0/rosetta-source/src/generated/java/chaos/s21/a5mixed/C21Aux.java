package chaos.s21.a5mixed;

import chaos.s21.a5mixed.meta.C21AuxMeta;
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
import java.util.Objects;

import static java.util.Optional.ofNullable;

/**
 * Self-contained helper - relocated by the import axis.
 * @version 1.0.0
 */
@RosettaDataType(value="C21Aux", builder=C21Aux.C21AuxBuilderImpl.class, version="1.0.0")
@RuneDataType(value="C21Aux", model="chaos", builder=C21Aux.C21AuxBuilderImpl.class, version="1.0.0")
public interface C21Aux extends RosettaModelObject {

	C21AuxMeta metaData = new C21AuxMeta();

	/*********************** Getter Methods  ***********************/
	String getAx();

	/*********************** Build Methods  ***********************/
	C21Aux build();
	
	C21Aux.C21AuxBuilder toBuilder();
	
	static C21Aux.C21AuxBuilder builder() {
		return new C21Aux.C21AuxBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C21Aux> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C21Aux> getType() {
		return C21Aux.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("ax"), String.class, getAx(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface C21AuxBuilder extends C21Aux, RosettaModelObjectBuilder {
		C21Aux.C21AuxBuilder setAx(String ax);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("ax"), String.class, getAx(), this);
		}
		

		C21Aux.C21AuxBuilder prune();
	}

	/*********************** Immutable Implementation of C21Aux  ***********************/
	class C21AuxImpl implements C21Aux {
		private final String ax;
		
		protected C21AuxImpl(C21Aux.C21AuxBuilder builder) {
			this.ax = builder.getAx();
		}
		
		@Override
		@RosettaAttribute("ax")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("ax")
		public String getAx() {
			return ax;
		}
		
		@Override
		public C21Aux build() {
			return this;
		}
		
		@Override
		public C21Aux.C21AuxBuilder toBuilder() {
			C21Aux.C21AuxBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C21Aux.C21AuxBuilder builder) {
			ofNullable(getAx()).ifPresent(builder::setAx);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C21Aux _that = getType().cast(o);
		
			if (!Objects.equals(ax, _that.getAx())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (ax != null ? ax.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C21Aux {" +
				"ax=" + this.ax +
			'}';
		}
	}

	/*********************** Builder Implementation of C21Aux  ***********************/
	class C21AuxBuilderImpl implements C21Aux.C21AuxBuilder {
	
		protected String ax;
		
		@Override
		@RosettaAttribute("ax")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("ax")
		public String getAx() {
			return ax;
		}
		
		@RosettaAttribute("ax")
		@Accessor(AccessorType.SETTER)
		@Required
		@RuneAttribute("ax")
		@Override
		public C21Aux.C21AuxBuilder setAx(String _ax) {
			this.ax = _ax == null ? null : _ax;
			return this;
		}
		
		@Override
		public C21Aux build() {
			return new C21Aux.C21AuxImpl(this);
		}
		
		@Override
		public C21Aux.C21AuxBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C21Aux.C21AuxBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getAx()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C21Aux.C21AuxBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C21Aux.C21AuxBuilder o = (C21Aux.C21AuxBuilder) other;
			
			
			merger.mergeBasic(getAx(), o.getAx(), this::setAx);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C21Aux _that = getType().cast(o);
		
			if (!Objects.equals(ax, _that.getAx())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (ax != null ? ax.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C21AuxBuilder {" +
				"ax=" + this.ax +
			'}';
		}
	}
}
