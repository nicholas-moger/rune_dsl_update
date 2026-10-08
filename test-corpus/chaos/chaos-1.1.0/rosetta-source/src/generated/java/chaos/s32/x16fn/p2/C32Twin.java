package chaos.s32.x16fn.p2;

import chaos.s32.x16fn.p2.meta.C32TwinMeta;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.annotations.Accessor;
import com.rosetta.model.lib.annotations.AccessorType;
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
 * RIVAL - a same-named TYPE in an imported namespace beside the seed&#39;s FUNCTION (the F17&#39; type-vs-function flavour).
 * @version 1.0.0
 */
@RosettaDataType(value="C32Twin", builder=C32Twin.C32TwinBuilderImpl.class, version="1.0.0")
@RuneDataType(value="C32Twin", model="chaos", builder=C32Twin.C32TwinBuilderImpl.class, version="1.0.0")
public interface C32Twin extends RosettaModelObject {

	C32TwinMeta metaData = new C32TwinMeta();

	/*********************** Getter Methods  ***********************/
	String getRivalMark();

	/*********************** Build Methods  ***********************/
	C32Twin build();
	
	C32Twin.C32TwinBuilder toBuilder();
	
	static C32Twin.C32TwinBuilder builder() {
		return new C32Twin.C32TwinBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C32Twin> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C32Twin> getType() {
		return C32Twin.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("rivalMark"), String.class, getRivalMark(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface C32TwinBuilder extends C32Twin, RosettaModelObjectBuilder {
		C32Twin.C32TwinBuilder setRivalMark(String rivalMark);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("rivalMark"), String.class, getRivalMark(), this);
		}
		

		C32Twin.C32TwinBuilder prune();
	}

	/*********************** Immutable Implementation of C32Twin  ***********************/
	class C32TwinImpl implements C32Twin {
		private final String rivalMark;
		
		protected C32TwinImpl(C32Twin.C32TwinBuilder builder) {
			this.rivalMark = builder.getRivalMark();
		}
		
		@Override
		@RosettaAttribute("rivalMark")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("rivalMark")
		public String getRivalMark() {
			return rivalMark;
		}
		
		@Override
		public C32Twin build() {
			return this;
		}
		
		@Override
		public C32Twin.C32TwinBuilder toBuilder() {
			C32Twin.C32TwinBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C32Twin.C32TwinBuilder builder) {
			ofNullable(getRivalMark()).ifPresent(builder::setRivalMark);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C32Twin _that = getType().cast(o);
		
			if (!Objects.equals(rivalMark, _that.getRivalMark())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (rivalMark != null ? rivalMark.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C32Twin {" +
				"rivalMark=" + this.rivalMark +
			'}';
		}
	}

	/*********************** Builder Implementation of C32Twin  ***********************/
	class C32TwinBuilderImpl implements C32Twin.C32TwinBuilder {
	
		protected String rivalMark;
		
		@Override
		@RosettaAttribute("rivalMark")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("rivalMark")
		public String getRivalMark() {
			return rivalMark;
		}
		
		@RosettaAttribute("rivalMark")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("rivalMark")
		@Override
		public C32Twin.C32TwinBuilder setRivalMark(String _rivalMark) {
			this.rivalMark = _rivalMark == null ? null : _rivalMark;
			return this;
		}
		
		@Override
		public C32Twin build() {
			return new C32Twin.C32TwinImpl(this);
		}
		
		@Override
		public C32Twin.C32TwinBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C32Twin.C32TwinBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getRivalMark()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C32Twin.C32TwinBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C32Twin.C32TwinBuilder o = (C32Twin.C32TwinBuilder) other;
			
			
			merger.mergeBasic(getRivalMark(), o.getRivalMark(), this::setRivalMark);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C32Twin _that = getType().cast(o);
		
			if (!Objects.equals(rivalMark, _that.getRivalMark())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (rivalMark != null ? rivalMark.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C32TwinBuilder {" +
				"rivalMark=" + this.rivalMark +
			'}';
		}
	}
}
