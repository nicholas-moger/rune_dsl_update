package chaos.s26.a1o3;

import chaos.s26.a1o3.meta.C26TagMeta;
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
 * Helper - relocated by the import axis.
 * @version 1.0.0
 */
@RosettaDataType(value="C26Tag", builder=C26Tag.C26TagBuilderImpl.class, version="1.0.0")
@RuneDataType(value="C26Tag", model="chaos", builder=C26Tag.C26TagBuilderImpl.class, version="1.0.0")
public interface C26Tag extends RosettaModelObject {

	C26TagMeta metaData = new C26TagMeta();

	/*********************** Getter Methods  ***********************/
	String getCode();

	/*********************** Build Methods  ***********************/
	C26Tag build();
	
	C26Tag.C26TagBuilder toBuilder();
	
	static C26Tag.C26TagBuilder builder() {
		return new C26Tag.C26TagBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C26Tag> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C26Tag> getType() {
		return C26Tag.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("code"), String.class, getCode(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface C26TagBuilder extends C26Tag, RosettaModelObjectBuilder {
		C26Tag.C26TagBuilder setCode(String code);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("code"), String.class, getCode(), this);
		}
		

		C26Tag.C26TagBuilder prune();
	}

	/*********************** Immutable Implementation of C26Tag  ***********************/
	class C26TagImpl implements C26Tag {
		private final String code;
		
		protected C26TagImpl(C26Tag.C26TagBuilder builder) {
			this.code = builder.getCode();
		}
		
		@Override
		@RosettaAttribute("code")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("code")
		public String getCode() {
			return code;
		}
		
		@Override
		public C26Tag build() {
			return this;
		}
		
		@Override
		public C26Tag.C26TagBuilder toBuilder() {
			C26Tag.C26TagBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C26Tag.C26TagBuilder builder) {
			ofNullable(getCode()).ifPresent(builder::setCode);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C26Tag _that = getType().cast(o);
		
			if (!Objects.equals(code, _that.getCode())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (code != null ? code.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C26Tag {" +
				"code=" + this.code +
			'}';
		}
	}

	/*********************** Builder Implementation of C26Tag  ***********************/
	class C26TagBuilderImpl implements C26Tag.C26TagBuilder {
	
		protected String code;
		
		@Override
		@RosettaAttribute("code")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("code")
		public String getCode() {
			return code;
		}
		
		@RosettaAttribute("code")
		@Accessor(AccessorType.SETTER)
		@Required
		@RuneAttribute("code")
		@Override
		public C26Tag.C26TagBuilder setCode(String _code) {
			this.code = _code == null ? null : _code;
			return this;
		}
		
		@Override
		public C26Tag build() {
			return new C26Tag.C26TagImpl(this);
		}
		
		@Override
		public C26Tag.C26TagBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C26Tag.C26TagBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getCode()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C26Tag.C26TagBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C26Tag.C26TagBuilder o = (C26Tag.C26TagBuilder) other;
			
			
			merger.mergeBasic(getCode(), o.getCode(), this::setCode);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C26Tag _that = getType().cast(o);
		
			if (!Objects.equals(code, _that.getCode())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (code != null ? code.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C26TagBuilder {" +
				"code=" + this.code +
			'}';
		}
	}
}
