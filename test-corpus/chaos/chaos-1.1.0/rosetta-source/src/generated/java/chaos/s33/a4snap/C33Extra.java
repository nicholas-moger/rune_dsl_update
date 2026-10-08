package chaos.s33.a4snap;

import chaos.s33.a4snap.meta.C33ExtraMeta;
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
 * @version 1.0.0-SNAPSHOT
 */
@RosettaDataType(value="C33Extra", builder=C33Extra.C33ExtraBuilderImpl.class, version="1.0.0-SNAPSHOT")
@RuneDataType(value="C33Extra", model="chaos", builder=C33Extra.C33ExtraBuilderImpl.class, version="1.0.0-SNAPSHOT")
public interface C33Extra extends RosettaModelObject {

	C33ExtraMeta metaData = new C33ExtraMeta();

	/*********************** Getter Methods  ***********************/
	String getMemo();

	/*********************** Build Methods  ***********************/
	C33Extra build();
	
	C33Extra.C33ExtraBuilder toBuilder();
	
	static C33Extra.C33ExtraBuilder builder() {
		return new C33Extra.C33ExtraBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C33Extra> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C33Extra> getType() {
		return C33Extra.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("memo"), String.class, getMemo(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface C33ExtraBuilder extends C33Extra, RosettaModelObjectBuilder {
		C33Extra.C33ExtraBuilder setMemo(String memo);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("memo"), String.class, getMemo(), this);
		}
		

		C33Extra.C33ExtraBuilder prune();
	}

	/*********************** Immutable Implementation of C33Extra  ***********************/
	class C33ExtraImpl implements C33Extra {
		private final String memo;
		
		protected C33ExtraImpl(C33Extra.C33ExtraBuilder builder) {
			this.memo = builder.getMemo();
		}
		
		@Override
		@RosettaAttribute("memo")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("memo")
		public String getMemo() {
			return memo;
		}
		
		@Override
		public C33Extra build() {
			return this;
		}
		
		@Override
		public C33Extra.C33ExtraBuilder toBuilder() {
			C33Extra.C33ExtraBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C33Extra.C33ExtraBuilder builder) {
			ofNullable(getMemo()).ifPresent(builder::setMemo);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C33Extra _that = getType().cast(o);
		
			if (!Objects.equals(memo, _that.getMemo())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (memo != null ? memo.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C33Extra {" +
				"memo=" + this.memo +
			'}';
		}
	}

	/*********************** Builder Implementation of C33Extra  ***********************/
	class C33ExtraBuilderImpl implements C33Extra.C33ExtraBuilder {
	
		protected String memo;
		
		@Override
		@RosettaAttribute("memo")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("memo")
		public String getMemo() {
			return memo;
		}
		
		@RosettaAttribute("memo")
		@Accessor(AccessorType.SETTER)
		@Required
		@RuneAttribute("memo")
		@Override
		public C33Extra.C33ExtraBuilder setMemo(String _memo) {
			this.memo = _memo == null ? null : _memo;
			return this;
		}
		
		@Override
		public C33Extra build() {
			return new C33Extra.C33ExtraImpl(this);
		}
		
		@Override
		public C33Extra.C33ExtraBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C33Extra.C33ExtraBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getMemo()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C33Extra.C33ExtraBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C33Extra.C33ExtraBuilder o = (C33Extra.C33ExtraBuilder) other;
			
			
			merger.mergeBasic(getMemo(), o.getMemo(), this::setMemo);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C33Extra _that = getType().cast(o);
		
			if (!Objects.equals(memo, _that.getMemo())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (memo != null ? memo.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C33ExtraBuilder {" +
				"memo=" + this.memo +
			'}';
		}
	}
}
