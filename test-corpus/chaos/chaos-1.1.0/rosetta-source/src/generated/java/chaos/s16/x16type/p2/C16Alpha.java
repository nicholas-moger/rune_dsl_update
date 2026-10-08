package chaos.s16.x16type.p2;

import chaos.s16.x16type.p2.meta.C16AlphaMeta;
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
 * RIVAL - a same-named global type in an imported namespace (the a2/P2 class).
 * @version 1.0.0
 */
@RosettaDataType(value="C16Alpha", builder=C16Alpha.C16AlphaBuilderImpl.class, version="1.0.0")
@RuneDataType(value="C16Alpha", model="chaos", builder=C16Alpha.C16AlphaBuilderImpl.class, version="1.0.0")
public interface C16Alpha extends RosettaModelObject {

	C16AlphaMeta metaData = new C16AlphaMeta();

	/*********************** Getter Methods  ***********************/
	String getRivalMark();

	/*********************** Build Methods  ***********************/
	C16Alpha build();
	
	C16Alpha.C16AlphaBuilder toBuilder();
	
	static C16Alpha.C16AlphaBuilder builder() {
		return new C16Alpha.C16AlphaBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C16Alpha> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C16Alpha> getType() {
		return C16Alpha.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("rivalMark"), String.class, getRivalMark(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface C16AlphaBuilder extends C16Alpha, RosettaModelObjectBuilder {
		C16Alpha.C16AlphaBuilder setRivalMark(String rivalMark);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("rivalMark"), String.class, getRivalMark(), this);
		}
		

		C16Alpha.C16AlphaBuilder prune();
	}

	/*********************** Immutable Implementation of C16Alpha  ***********************/
	class C16AlphaImpl implements C16Alpha {
		private final String rivalMark;
		
		protected C16AlphaImpl(C16Alpha.C16AlphaBuilder builder) {
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
		public C16Alpha build() {
			return this;
		}
		
		@Override
		public C16Alpha.C16AlphaBuilder toBuilder() {
			C16Alpha.C16AlphaBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C16Alpha.C16AlphaBuilder builder) {
			ofNullable(getRivalMark()).ifPresent(builder::setRivalMark);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C16Alpha _that = getType().cast(o);
		
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
			return "C16Alpha {" +
				"rivalMark=" + this.rivalMark +
			'}';
		}
	}

	/*********************** Builder Implementation of C16Alpha  ***********************/
	class C16AlphaBuilderImpl implements C16Alpha.C16AlphaBuilder {
	
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
		public C16Alpha.C16AlphaBuilder setRivalMark(String _rivalMark) {
			this.rivalMark = _rivalMark == null ? null : _rivalMark;
			return this;
		}
		
		@Override
		public C16Alpha build() {
			return new C16Alpha.C16AlphaImpl(this);
		}
		
		@Override
		public C16Alpha.C16AlphaBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C16Alpha.C16AlphaBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getRivalMark()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C16Alpha.C16AlphaBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C16Alpha.C16AlphaBuilder o = (C16Alpha.C16AlphaBuilder) other;
			
			
			merger.mergeBasic(getRivalMark(), o.getRivalMark(), this::setRivalMark);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C16Alpha _that = getType().cast(o);
		
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
			return "C16AlphaBuilder {" +
				"rivalMark=" + this.rivalMark +
			'}';
		}
	}
}
