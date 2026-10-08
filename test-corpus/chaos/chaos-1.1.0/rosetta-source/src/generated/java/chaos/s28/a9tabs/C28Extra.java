package chaos.s28.a9tabs;

import chaos.s28.a9tabs.meta.C28ExtraMeta;
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
@RosettaDataType(value="C28Extra", builder=C28Extra.C28ExtraBuilderImpl.class, version="1.0.0")
@RuneDataType(value="C28Extra", model="chaos", builder=C28Extra.C28ExtraBuilderImpl.class, version="1.0.0")
public interface C28Extra extends RosettaModelObject {

	C28ExtraMeta metaData = new C28ExtraMeta();

	/*********************** Getter Methods  ***********************/
	String getMemo();

	/*********************** Build Methods  ***********************/
	C28Extra build();
	
	C28Extra.C28ExtraBuilder toBuilder();
	
	static C28Extra.C28ExtraBuilder builder() {
		return new C28Extra.C28ExtraBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C28Extra> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C28Extra> getType() {
		return C28Extra.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("memo"), String.class, getMemo(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface C28ExtraBuilder extends C28Extra, RosettaModelObjectBuilder {
		C28Extra.C28ExtraBuilder setMemo(String memo);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("memo"), String.class, getMemo(), this);
		}
		

		C28Extra.C28ExtraBuilder prune();
	}

	/*********************** Immutable Implementation of C28Extra  ***********************/
	class C28ExtraImpl implements C28Extra {
		private final String memo;
		
		protected C28ExtraImpl(C28Extra.C28ExtraBuilder builder) {
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
		public C28Extra build() {
			return this;
		}
		
		@Override
		public C28Extra.C28ExtraBuilder toBuilder() {
			C28Extra.C28ExtraBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C28Extra.C28ExtraBuilder builder) {
			ofNullable(getMemo()).ifPresent(builder::setMemo);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C28Extra _that = getType().cast(o);
		
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
			return "C28Extra {" +
				"memo=" + this.memo +
			'}';
		}
	}

	/*********************** Builder Implementation of C28Extra  ***********************/
	class C28ExtraBuilderImpl implements C28Extra.C28ExtraBuilder {
	
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
		public C28Extra.C28ExtraBuilder setMemo(String _memo) {
			this.memo = _memo == null ? null : _memo;
			return this;
		}
		
		@Override
		public C28Extra build() {
			return new C28Extra.C28ExtraImpl(this);
		}
		
		@Override
		public C28Extra.C28ExtraBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C28Extra.C28ExtraBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getMemo()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C28Extra.C28ExtraBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C28Extra.C28ExtraBuilder o = (C28Extra.C28ExtraBuilder) other;
			
			
			merger.mergeBasic(getMemo(), o.getMemo(), this::setMemo);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C28Extra _that = getType().cast(o);
		
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
			return "C28ExtraBuilder {" +
				"memo=" + this.memo +
			'}';
		}
	}
}
