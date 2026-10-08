package holdout.voiddeeptok;

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
import com.rosetta.model.metafields.FieldWithMetaVoid;
import holdout.voiddeeptok.meta.HolderAMeta;
import java.util.Objects;

import static java.util.Optional.ofNullable;

/**
 * One option of the choice, carrying the scheme-annotated Void feature.
 * @version 0.0.0
 */
@RosettaDataType(value="HolderA", builder=HolderA.HolderABuilderImpl.class, version="0.0.0")
@RuneDataType(value="HolderA", model="holdout", builder=HolderA.HolderABuilderImpl.class, version="0.0.0")
public interface HolderA extends RosettaModelObject {

	HolderAMeta metaData = new HolderAMeta();

	/*********************** Getter Methods  ***********************/
	FieldWithMetaVoid getTok();

	/*********************** Build Methods  ***********************/
	HolderA build();
	
	HolderA.HolderABuilder toBuilder();
	
	static HolderA.HolderABuilder builder() {
		return new HolderA.HolderABuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends HolderA> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends HolderA> getType() {
		return HolderA.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processRosetta(path.newSubPath("tok"), processor, FieldWithMetaVoid.class, getTok());
	}
	

	/*********************** Builder Interface  ***********************/
	interface HolderABuilder extends HolderA, RosettaModelObjectBuilder {
		FieldWithMetaVoid.FieldWithMetaVoidBuilder getOrCreateTok();
		@Override
		FieldWithMetaVoid.FieldWithMetaVoidBuilder getTok();
		HolderA.HolderABuilder setTok(FieldWithMetaVoid tok);
		HolderA.HolderABuilder setTokValue(Void tok);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processRosetta(path.newSubPath("tok"), processor, FieldWithMetaVoid.FieldWithMetaVoidBuilder.class, getTok());
		}
		

		HolderA.HolderABuilder prune();
	}

	/*********************** Immutable Implementation of HolderA  ***********************/
	class HolderAImpl implements HolderA {
		private final FieldWithMetaVoid tok;
		
		protected HolderAImpl(HolderA.HolderABuilder builder) {
			this.tok = ofNullable(builder.getTok()).map(f->f.build()).orElse(null);
		}
		
		@Override
		@RosettaAttribute("tok")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("tok")
		public FieldWithMetaVoid getTok() {
			return tok;
		}
		
		@Override
		public HolderA build() {
			return this;
		}
		
		@Override
		public HolderA.HolderABuilder toBuilder() {
			HolderA.HolderABuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(HolderA.HolderABuilder builder) {
			ofNullable(getTok()).ifPresent(builder::setTok);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			HolderA _that = getType().cast(o);
		
			if (!Objects.equals(tok, _that.getTok())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (tok != null ? tok.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "HolderA {" +
				"tok=" + this.tok +
			'}';
		}
	}

	/*********************** Builder Implementation of HolderA  ***********************/
	class HolderABuilderImpl implements HolderA.HolderABuilder {
	
		protected FieldWithMetaVoid.FieldWithMetaVoidBuilder tok;
		
		@Override
		@RosettaAttribute("tok")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("tok")
		public FieldWithMetaVoid.FieldWithMetaVoidBuilder getTok() {
			return tok;
		}
		
		@Override
		public FieldWithMetaVoid.FieldWithMetaVoidBuilder getOrCreateTok() {
			FieldWithMetaVoid.FieldWithMetaVoidBuilder result;
			if (tok!=null) {
				result = tok;
			}
			else {
				result = tok = FieldWithMetaVoid.builder();
			}
			
			return result;
		}
		
		@RosettaAttribute("tok")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("tok")
		@Override
		public HolderA.HolderABuilder setTok(FieldWithMetaVoid _tok) {
			this.tok = _tok == null ? null : _tok.toBuilder();
			return this;
		}
		
		@Override
		public HolderA.HolderABuilder setTokValue(Void _tok) {
			this.getOrCreateTok().setValue(_tok);
			return this;
		}
		
		@Override
		public HolderA build() {
			return new HolderA.HolderAImpl(this);
		}
		
		@Override
		public HolderA.HolderABuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public HolderA.HolderABuilder prune() {
			if (tok!=null && !tok.prune().hasData()) tok = null;
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getTok()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public HolderA.HolderABuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			HolderA.HolderABuilder o = (HolderA.HolderABuilder) other;
			
			merger.mergeRosetta(getTok(), o.getTok(), this::setTok);
			
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			HolderA _that = getType().cast(o);
		
			if (!Objects.equals(tok, _that.getTok())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (tok != null ? tok.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "HolderABuilder {" +
				"tok=" + this.tok +
			'}';
		}
	}
}
