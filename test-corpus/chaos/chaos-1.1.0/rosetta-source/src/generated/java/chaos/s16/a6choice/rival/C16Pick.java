package chaos.s16.a6choice.rival;

import chaos.s16.a6choice.rival.meta.C16PickMeta;
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
 * RIVAL - a same-named global choice in an imported namespace (the P3 class).
 * @version 1.0.0
 */
@RosettaDataType(value="C16Pick", builder=C16Pick.C16PickBuilderImpl.class, version="1.0.0")
@RuneDataType(value="C16Pick", model="chaos", builder=C16Pick.C16PickBuilderImpl.class, version="1.0.0")
@RuneChoiceType
public interface C16Pick extends RosettaModelObject {

	C16PickMeta metaData = new C16PickMeta();

	/*********************** Getter Methods  ***********************/
	C16RivalOpt getC16RivalOpt();

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
		processRosetta(path.newSubPath("C16RivalOpt"), processor, C16RivalOpt.class, getC16RivalOpt());
	}
	

	/*********************** Builder Interface  ***********************/
	interface C16PickBuilder extends C16Pick, RosettaModelObjectBuilder {
		C16RivalOpt.C16RivalOptBuilder getOrCreateC16RivalOpt();
		@Override
		C16RivalOpt.C16RivalOptBuilder getC16RivalOpt();
		C16Pick.C16PickBuilder setC16RivalOpt(C16RivalOpt _C16RivalOpt);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processRosetta(path.newSubPath("C16RivalOpt"), processor, C16RivalOpt.C16RivalOptBuilder.class, getC16RivalOpt());
		}
		

		C16Pick.C16PickBuilder prune();
	}

	/*********************** Immutable Implementation of C16Pick  ***********************/
	class C16PickImpl implements C16Pick {
		private final C16RivalOpt c16RivalOpt;
		
		protected C16PickImpl(C16Pick.C16PickBuilder builder) {
			this.c16RivalOpt = ofNullable(builder.getC16RivalOpt()).map(f->f.build()).orElse(null);
		}
		
		@Override
		@RosettaAttribute("C16RivalOpt")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("C16RivalOpt")
		public C16RivalOpt getC16RivalOpt() {
			return c16RivalOpt;
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
			ofNullable(getC16RivalOpt()).ifPresent(builder::setC16RivalOpt);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C16Pick _that = getType().cast(o);
		
			if (!Objects.equals(c16RivalOpt, _that.getC16RivalOpt())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (c16RivalOpt != null ? c16RivalOpt.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C16Pick {" +
				"C16RivalOpt=" + this.c16RivalOpt +
			'}';
		}
	}

	/*********************** Builder Implementation of C16Pick  ***********************/
	class C16PickBuilderImpl implements C16Pick.C16PickBuilder {
	
		protected C16RivalOpt.C16RivalOptBuilder c16RivalOpt;
		
		@Override
		@RosettaAttribute("C16RivalOpt")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("C16RivalOpt")
		public C16RivalOpt.C16RivalOptBuilder getC16RivalOpt() {
			return c16RivalOpt;
		}
		
		@Override
		public C16RivalOpt.C16RivalOptBuilder getOrCreateC16RivalOpt() {
			C16RivalOpt.C16RivalOptBuilder result;
			if (c16RivalOpt!=null) {
				result = c16RivalOpt;
			}
			else {
				result = c16RivalOpt = C16RivalOpt.builder();
			}
			
			return result;
		}
		
		@RosettaAttribute("C16RivalOpt")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("C16RivalOpt")
		@Override
		public C16Pick.C16PickBuilder setC16RivalOpt(C16RivalOpt _c16RivalOpt) {
			this.c16RivalOpt = _c16RivalOpt == null ? null : _c16RivalOpt.toBuilder();
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
			if (c16RivalOpt!=null && !c16RivalOpt.prune().hasData()) c16RivalOpt = null;
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getC16RivalOpt()!=null && getC16RivalOpt().hasData()) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C16Pick.C16PickBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C16Pick.C16PickBuilder o = (C16Pick.C16PickBuilder) other;
			
			merger.mergeRosetta(getC16RivalOpt(), o.getC16RivalOpt(), this::setC16RivalOpt);
			
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C16Pick _that = getType().cast(o);
		
			if (!Objects.equals(c16RivalOpt, _that.getC16RivalOpt())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (c16RivalOpt != null ? c16RivalOpt.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C16PickBuilder {" +
				"C16RivalOpt=" + this.c16RivalOpt +
			'}';
		}
	}
}
