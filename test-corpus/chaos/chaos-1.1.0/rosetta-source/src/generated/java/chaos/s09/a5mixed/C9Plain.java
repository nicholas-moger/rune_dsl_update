package chaos.s09.a5mixed;

import chaos.s09.a5mixed.meta.C9PlainMeta;
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
@RosettaDataType(value="C9Plain", builder=C9Plain.C9PlainBuilderImpl.class, version="1.0.0")
@RuneDataType(value="C9Plain", model="chaos", builder=C9Plain.C9PlainBuilderImpl.class, version="1.0.0")
public interface C9Plain extends RosettaModelObject {

	C9PlainMeta metaData = new C9PlainMeta();

	/*********************** Getter Methods  ***********************/
	String getP();

	/*********************** Build Methods  ***********************/
	C9Plain build();
	
	C9Plain.C9PlainBuilder toBuilder();
	
	static C9Plain.C9PlainBuilder builder() {
		return new C9Plain.C9PlainBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C9Plain> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C9Plain> getType() {
		return C9Plain.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("p"), String.class, getP(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface C9PlainBuilder extends C9Plain, RosettaModelObjectBuilder {
		C9Plain.C9PlainBuilder setP(String p);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("p"), String.class, getP(), this);
		}
		

		C9Plain.C9PlainBuilder prune();
	}

	/*********************** Immutable Implementation of C9Plain  ***********************/
	class C9PlainImpl implements C9Plain {
		private final String p;
		
		protected C9PlainImpl(C9Plain.C9PlainBuilder builder) {
			this.p = builder.getP();
		}
		
		@Override
		@RosettaAttribute("p")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("p")
		public String getP() {
			return p;
		}
		
		@Override
		public C9Plain build() {
			return this;
		}
		
		@Override
		public C9Plain.C9PlainBuilder toBuilder() {
			C9Plain.C9PlainBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C9Plain.C9PlainBuilder builder) {
			ofNullable(getP()).ifPresent(builder::setP);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C9Plain _that = getType().cast(o);
		
			if (!Objects.equals(p, _that.getP())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (p != null ? p.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C9Plain {" +
				"p=" + this.p +
			'}';
		}
	}

	/*********************** Builder Implementation of C9Plain  ***********************/
	class C9PlainBuilderImpl implements C9Plain.C9PlainBuilder {
	
		protected String p;
		
		@Override
		@RosettaAttribute("p")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("p")
		public String getP() {
			return p;
		}
		
		@RosettaAttribute("p")
		@Accessor(AccessorType.SETTER)
		@Required
		@RuneAttribute("p")
		@Override
		public C9Plain.C9PlainBuilder setP(String _p) {
			this.p = _p == null ? null : _p;
			return this;
		}
		
		@Override
		public C9Plain build() {
			return new C9Plain.C9PlainImpl(this);
		}
		
		@Override
		public C9Plain.C9PlainBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C9Plain.C9PlainBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getP()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C9Plain.C9PlainBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C9Plain.C9PlainBuilder o = (C9Plain.C9PlainBuilder) other;
			
			
			merger.mergeBasic(getP(), o.getP(), this::setP);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C9Plain _that = getType().cast(o);
		
			if (!Objects.equals(p, _that.getP())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (p != null ? p.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C9PlainBuilder {" +
				"p=" + this.p +
			'}';
		}
	}
}
