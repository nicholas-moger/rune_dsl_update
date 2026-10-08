package chaos.s01.a1o3;

import chaos.s01.a1o3.meta.C1RefMeta;
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
 * A referenced component the import-style axis relocates.
 * @version 1.0.0
 */
@RosettaDataType(value="C1Ref", builder=C1Ref.C1RefBuilderImpl.class, version="1.0.0")
@RuneDataType(value="C1Ref", model="chaos", builder=C1Ref.C1RefBuilderImpl.class, version="1.0.0")
public interface C1Ref extends RosettaModelObject {

	C1RefMeta metaData = new C1RefMeta();

	/*********************** Getter Methods  ***********************/
	String getRefCode();

	/*********************** Build Methods  ***********************/
	C1Ref build();
	
	C1Ref.C1RefBuilder toBuilder();
	
	static C1Ref.C1RefBuilder builder() {
		return new C1Ref.C1RefBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C1Ref> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C1Ref> getType() {
		return C1Ref.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("refCode"), String.class, getRefCode(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface C1RefBuilder extends C1Ref, RosettaModelObjectBuilder {
		C1Ref.C1RefBuilder setRefCode(String refCode);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("refCode"), String.class, getRefCode(), this);
		}
		

		C1Ref.C1RefBuilder prune();
	}

	/*********************** Immutable Implementation of C1Ref  ***********************/
	class C1RefImpl implements C1Ref {
		private final String refCode;
		
		protected C1RefImpl(C1Ref.C1RefBuilder builder) {
			this.refCode = builder.getRefCode();
		}
		
		@Override
		@RosettaAttribute("refCode")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("refCode")
		public String getRefCode() {
			return refCode;
		}
		
		@Override
		public C1Ref build() {
			return this;
		}
		
		@Override
		public C1Ref.C1RefBuilder toBuilder() {
			C1Ref.C1RefBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C1Ref.C1RefBuilder builder) {
			ofNullable(getRefCode()).ifPresent(builder::setRefCode);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C1Ref _that = getType().cast(o);
		
			if (!Objects.equals(refCode, _that.getRefCode())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (refCode != null ? refCode.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C1Ref {" +
				"refCode=" + this.refCode +
			'}';
		}
	}

	/*********************** Builder Implementation of C1Ref  ***********************/
	class C1RefBuilderImpl implements C1Ref.C1RefBuilder {
	
		protected String refCode;
		
		@Override
		@RosettaAttribute("refCode")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("refCode")
		public String getRefCode() {
			return refCode;
		}
		
		@RosettaAttribute("refCode")
		@Accessor(AccessorType.SETTER)
		@Required
		@RuneAttribute("refCode")
		@Override
		public C1Ref.C1RefBuilder setRefCode(String _refCode) {
			this.refCode = _refCode == null ? null : _refCode;
			return this;
		}
		
		@Override
		public C1Ref build() {
			return new C1Ref.C1RefImpl(this);
		}
		
		@Override
		public C1Ref.C1RefBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C1Ref.C1RefBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getRefCode()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C1Ref.C1RefBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C1Ref.C1RefBuilder o = (C1Ref.C1RefBuilder) other;
			
			
			merger.mergeBasic(getRefCode(), o.getRefCode(), this::setRefCode);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C1Ref _that = getType().cast(o);
		
			if (!Objects.equals(refCode, _that.getRefCode())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (refCode != null ? refCode.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C1RefBuilder {" +
				"refCode=" + this.refCode +
			'}';
		}
	}
}
