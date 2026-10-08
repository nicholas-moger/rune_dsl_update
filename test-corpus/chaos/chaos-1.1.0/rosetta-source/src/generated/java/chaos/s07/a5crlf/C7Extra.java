package chaos.s07.a5crlf;

import chaos.s07.a5crlf.meta.C7ExtraMeta;
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
@RosettaDataType(value="C7Extra", builder=C7Extra.C7ExtraBuilderImpl.class, version="1.0.0")
@RuneDataType(value="C7Extra", model="chaos", builder=C7Extra.C7ExtraBuilderImpl.class, version="1.0.0")
public interface C7Extra extends RosettaModelObject {

	C7ExtraMeta metaData = new C7ExtraMeta();

	/*********************** Getter Methods  ***********************/
	String getMemo();

	/*********************** Build Methods  ***********************/
	C7Extra build();
	
	C7Extra.C7ExtraBuilder toBuilder();
	
	static C7Extra.C7ExtraBuilder builder() {
		return new C7Extra.C7ExtraBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C7Extra> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C7Extra> getType() {
		return C7Extra.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("memo"), String.class, getMemo(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface C7ExtraBuilder extends C7Extra, RosettaModelObjectBuilder {
		C7Extra.C7ExtraBuilder setMemo(String memo);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("memo"), String.class, getMemo(), this);
		}
		

		C7Extra.C7ExtraBuilder prune();
	}

	/*********************** Immutable Implementation of C7Extra  ***********************/
	class C7ExtraImpl implements C7Extra {
		private final String memo;
		
		protected C7ExtraImpl(C7Extra.C7ExtraBuilder builder) {
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
		public C7Extra build() {
			return this;
		}
		
		@Override
		public C7Extra.C7ExtraBuilder toBuilder() {
			C7Extra.C7ExtraBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C7Extra.C7ExtraBuilder builder) {
			ofNullable(getMemo()).ifPresent(builder::setMemo);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C7Extra _that = getType().cast(o);
		
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
			return "C7Extra {" +
				"memo=" + this.memo +
			'}';
		}
	}

	/*********************** Builder Implementation of C7Extra  ***********************/
	class C7ExtraBuilderImpl implements C7Extra.C7ExtraBuilder {
	
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
		public C7Extra.C7ExtraBuilder setMemo(String _memo) {
			this.memo = _memo == null ? null : _memo;
			return this;
		}
		
		@Override
		public C7Extra build() {
			return new C7Extra.C7ExtraImpl(this);
		}
		
		@Override
		public C7Extra.C7ExtraBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C7Extra.C7ExtraBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getMemo()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C7Extra.C7ExtraBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C7Extra.C7ExtraBuilder o = (C7Extra.C7ExtraBuilder) other;
			
			
			merger.mergeBasic(getMemo(), o.getMemo(), this::setMemo);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C7Extra _that = getType().cast(o);
		
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
			return "C7ExtraBuilder {" +
				"memo=" + this.memo +
			'}';
		}
	}
}
