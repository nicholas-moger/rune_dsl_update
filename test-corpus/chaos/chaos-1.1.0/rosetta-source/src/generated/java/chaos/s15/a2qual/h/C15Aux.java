package chaos.s15.a2qual.h;

import chaos.s15.a2qual.h.meta.C15AuxMeta;
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
@RosettaDataType(value="C15Aux", builder=C15Aux.C15AuxBuilderImpl.class, version="1.0.0")
@RuneDataType(value="C15Aux", model="chaos", builder=C15Aux.C15AuxBuilderImpl.class, version="1.0.0")
public interface C15Aux extends RosettaModelObject {

	C15AuxMeta metaData = new C15AuxMeta();

	/*********************** Getter Methods  ***********************/
	String getAx();

	/*********************** Build Methods  ***********************/
	C15Aux build();
	
	C15Aux.C15AuxBuilder toBuilder();
	
	static C15Aux.C15AuxBuilder builder() {
		return new C15Aux.C15AuxBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C15Aux> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C15Aux> getType() {
		return C15Aux.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("ax"), String.class, getAx(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface C15AuxBuilder extends C15Aux, RosettaModelObjectBuilder {
		C15Aux.C15AuxBuilder setAx(String ax);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("ax"), String.class, getAx(), this);
		}
		

		C15Aux.C15AuxBuilder prune();
	}

	/*********************** Immutable Implementation of C15Aux  ***********************/
	class C15AuxImpl implements C15Aux {
		private final String ax;
		
		protected C15AuxImpl(C15Aux.C15AuxBuilder builder) {
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
		public C15Aux build() {
			return this;
		}
		
		@Override
		public C15Aux.C15AuxBuilder toBuilder() {
			C15Aux.C15AuxBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C15Aux.C15AuxBuilder builder) {
			ofNullable(getAx()).ifPresent(builder::setAx);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C15Aux _that = getType().cast(o);
		
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
			return "C15Aux {" +
				"ax=" + this.ax +
			'}';
		}
	}

	/*********************** Builder Implementation of C15Aux  ***********************/
	class C15AuxBuilderImpl implements C15Aux.C15AuxBuilder {
	
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
		public C15Aux.C15AuxBuilder setAx(String _ax) {
			this.ax = _ax == null ? null : _ax;
			return this;
		}
		
		@Override
		public C15Aux build() {
			return new C15Aux.C15AuxImpl(this);
		}
		
		@Override
		public C15Aux.C15AuxBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C15Aux.C15AuxBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getAx()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C15Aux.C15AuxBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C15Aux.C15AuxBuilder o = (C15Aux.C15AuxBuilder) other;
			
			
			merger.mergeBasic(getAx(), o.getAx(), this::setAx);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C15Aux _that = getType().cast(o);
		
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
			return "C15AuxBuilder {" +
				"ax=" + this.ax +
			'}';
		}
	}
}
