package chaos.s16.x36type.p1;

import chaos.s16.x36type.p1.meta.C16PickMeta;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.annotations.Accessor;
import com.rosetta.model.lib.annotations.AccessorType;
import com.rosetta.model.lib.annotations.RosettaAttribute;
import com.rosetta.model.lib.annotations.RosettaDataType;
import com.rosetta.model.lib.annotations.RuneAttribute;
import com.rosetta.model.lib.annotations.RuneChoiceType;
import com.rosetta.model.lib.annotations.RuneDataType;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.BuilderMerger;
import com.rosetta.model.lib.process.BuilderProcessor;
import com.rosetta.model.lib.process.Processor;
import java.util.Objects;

import static java.util.Optional.ofNullable;

/**
 * The choice whose bare option names the rivals collide with.
 * @version 1.0.0
 */
@RosettaDataType(value="C16Pick", builder=C16Pick.C16PickBuilderImpl.class, version="1.0.0")
@RuneDataType(value="C16Pick", model="chaos", builder=C16Pick.C16PickBuilderImpl.class, version="1.0.0")
@RuneChoiceType
public interface C16Pick extends RosettaModelObject {

	C16PickMeta metaData = new C16PickMeta();

	/*********************** Getter Methods  ***********************/
	C16Alpha getC16Alpha();
	C16Beta getC16Beta();

	/*********************** Build Methods  ***********************/
	C16Pick build();
	
	C16Pick.C16PickBuilder toBuilder();
	
	static C16Pick.C16PickBuilder builder() {
		return new C16Pick.C16PickBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C16Pick> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C16Pick> getType() {
		return C16Pick.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processRosetta(path.newSubPath("C16Alpha"), processor, C16Alpha.class, getC16Alpha());
		processRosetta(path.newSubPath("C16Beta"), processor, C16Beta.class, getC16Beta());
	}
	

	/*********************** Builder Interface  ***********************/
	interface C16PickBuilder extends C16Pick, RosettaModelObjectBuilder {
		C16Alpha.C16AlphaBuilder getOrCreateC16Alpha();
		@Override
		C16Alpha.C16AlphaBuilder getC16Alpha();
		C16Beta.C16BetaBuilder getOrCreateC16Beta();
		@Override
		C16Beta.C16BetaBuilder getC16Beta();
		C16Pick.C16PickBuilder setC16Alpha(C16Alpha _C16Alpha);
		C16Pick.C16PickBuilder setC16Beta(C16Beta _C16Beta);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processRosetta(path.newSubPath("C16Alpha"), processor, C16Alpha.C16AlphaBuilder.class, getC16Alpha());
			processRosetta(path.newSubPath("C16Beta"), processor, C16Beta.C16BetaBuilder.class, getC16Beta());
		}
		

		C16Pick.C16PickBuilder prune();
	}

	/*********************** Immutable Implementation of C16Pick  ***********************/
	class C16PickImpl implements C16Pick {
		private final C16Alpha c16Alpha;
		private final C16Beta c16Beta;
		
		protected C16PickImpl(C16Pick.C16PickBuilder builder) {
			this.c16Alpha = ofNullable(builder.getC16Alpha()).map(f->f.build()).orElse(null);
			this.c16Beta = ofNullable(builder.getC16Beta()).map(f->f.build()).orElse(null);
		}
		
		@Override
		@RosettaAttribute("C16Alpha")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("C16Alpha")
		public C16Alpha getC16Alpha() {
			return c16Alpha;
		}
		
		@Override
		@RosettaAttribute("C16Beta")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("C16Beta")
		public C16Beta getC16Beta() {
			return c16Beta;
		}
		
		@Override
		public C16Pick build() {
			return this;
		}
		
		@Override
		public C16Pick.C16PickBuilder toBuilder() {
			C16Pick.C16PickBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C16Pick.C16PickBuilder builder) {
			ofNullable(getC16Alpha()).ifPresent(builder::setC16Alpha);
			ofNullable(getC16Beta()).ifPresent(builder::setC16Beta);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C16Pick _that = getType().cast(o);
		
			if (!Objects.equals(c16Alpha, _that.getC16Alpha())) return false;
			if (!Objects.equals(c16Beta, _that.getC16Beta())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (c16Alpha != null ? c16Alpha.hashCode() : 0);
			_result = 31 * _result + (c16Beta != null ? c16Beta.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C16Pick {" +
				"C16Alpha=" + this.c16Alpha + ", " +
				"C16Beta=" + this.c16Beta +
			'}';
		}
	}

	/*********************** Builder Implementation of C16Pick  ***********************/
	class C16PickBuilderImpl implements C16Pick.C16PickBuilder {
	
		protected C16Alpha.C16AlphaBuilder c16Alpha;
		protected C16Beta.C16BetaBuilder c16Beta;
		
		@Override
		@RosettaAttribute("C16Alpha")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("C16Alpha")
		public C16Alpha.C16AlphaBuilder getC16Alpha() {
			return c16Alpha;
		}
		
		@Override
		public C16Alpha.C16AlphaBuilder getOrCreateC16Alpha() {
			C16Alpha.C16AlphaBuilder result;
			if (c16Alpha!=null) {
				result = c16Alpha;
			}
			else {
				result = c16Alpha = C16Alpha.builder();
			}
			
			return result;
		}
		
		@Override
		@RosettaAttribute("C16Beta")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("C16Beta")
		public C16Beta.C16BetaBuilder getC16Beta() {
			return c16Beta;
		}
		
		@Override
		public C16Beta.C16BetaBuilder getOrCreateC16Beta() {
			C16Beta.C16BetaBuilder result;
			if (c16Beta!=null) {
				result = c16Beta;
			}
			else {
				result = c16Beta = C16Beta.builder();
			}
			
			return result;
		}
		
		@RosettaAttribute("C16Alpha")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("C16Alpha")
		@Override
		public C16Pick.C16PickBuilder setC16Alpha(C16Alpha _c16Alpha) {
			this.c16Alpha = _c16Alpha == null ? null : _c16Alpha.toBuilder();
			return this;
		}
		
		@RosettaAttribute("C16Beta")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("C16Beta")
		@Override
		public C16Pick.C16PickBuilder setC16Beta(C16Beta _c16Beta) {
			this.c16Beta = _c16Beta == null ? null : _c16Beta.toBuilder();
			return this;
		}
		
		@Override
		public C16Pick build() {
			return new C16Pick.C16PickImpl(this);
		}
		
		@Override
		public C16Pick.C16PickBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C16Pick.C16PickBuilder prune() {
			if (c16Alpha!=null && !c16Alpha.prune().hasData()) c16Alpha = null;
			if (c16Beta!=null && !c16Beta.prune().hasData()) c16Beta = null;
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getC16Alpha()!=null && getC16Alpha().hasData()) return true;
			if (getC16Beta()!=null && getC16Beta().hasData()) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C16Pick.C16PickBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C16Pick.C16PickBuilder o = (C16Pick.C16PickBuilder) other;
			
			merger.mergeRosetta(getC16Alpha(), o.getC16Alpha(), this::setC16Alpha);
			merger.mergeRosetta(getC16Beta(), o.getC16Beta(), this::setC16Beta);
			
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C16Pick _that = getType().cast(o);
		
			if (!Objects.equals(c16Alpha, _that.getC16Alpha())) return false;
			if (!Objects.equals(c16Beta, _that.getC16Beta())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (c16Alpha != null ? c16Alpha.hashCode() : 0);
			_result = 31 * _result + (c16Beta != null ? c16Beta.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C16PickBuilder {" +
				"C16Alpha=" + this.c16Alpha + ", " +
				"C16Beta=" + this.c16Beta +
			'}';
		}
	}
}
