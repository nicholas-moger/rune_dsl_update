package chaos.s14.a4none;

import chaos.s14.a4none.meta.C14AuxMeta;
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
 * @version 0.0.0
 */
@RosettaDataType(value="C14Aux", builder=C14Aux.C14AuxBuilderImpl.class, version="0.0.0")
@RuneDataType(value="C14Aux", model="chaos", builder=C14Aux.C14AuxBuilderImpl.class, version="0.0.0")
public interface C14Aux extends RosettaModelObject {

	C14AuxMeta metaData = new C14AuxMeta();

	/*********************** Getter Methods  ***********************/
	String getAx();

	/*********************** Build Methods  ***********************/
	C14Aux build();
	
	C14Aux.C14AuxBuilder toBuilder();
	
	static C14Aux.C14AuxBuilder builder() {
		return new C14Aux.C14AuxBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C14Aux> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C14Aux> getType() {
		return C14Aux.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("ax"), String.class, getAx(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface C14AuxBuilder extends C14Aux, RosettaModelObjectBuilder {
		C14Aux.C14AuxBuilder setAx(String ax);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("ax"), String.class, getAx(), this);
		}
		

		C14Aux.C14AuxBuilder prune();
	}

	/*********************** Immutable Implementation of C14Aux  ***********************/
	class C14AuxImpl implements C14Aux {
		private final String ax;
		
		protected C14AuxImpl(C14Aux.C14AuxBuilder builder) {
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
		public C14Aux build() {
			return this;
		}
		
		@Override
		public C14Aux.C14AuxBuilder toBuilder() {
			C14Aux.C14AuxBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C14Aux.C14AuxBuilder builder) {
			ofNullable(getAx()).ifPresent(builder::setAx);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C14Aux _that = getType().cast(o);
		
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
			return "C14Aux {" +
				"ax=" + this.ax +
			'}';
		}
	}

	/*********************** Builder Implementation of C14Aux  ***********************/
	class C14AuxBuilderImpl implements C14Aux.C14AuxBuilder {
	
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
		public C14Aux.C14AuxBuilder setAx(String _ax) {
			this.ax = _ax == null ? null : _ax;
			return this;
		}
		
		@Override
		public C14Aux build() {
			return new C14Aux.C14AuxImpl(this);
		}
		
		@Override
		public C14Aux.C14AuxBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C14Aux.C14AuxBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getAx()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C14Aux.C14AuxBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C14Aux.C14AuxBuilder o = (C14Aux.C14AuxBuilder) other;
			
			
			merger.mergeBasic(getAx(), o.getAx(), this::setAx);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C14Aux _that = getType().cast(o);
		
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
			return "C14AuxBuilder {" +
				"ax=" + this.ax +
			'}';
		}
	}
}
