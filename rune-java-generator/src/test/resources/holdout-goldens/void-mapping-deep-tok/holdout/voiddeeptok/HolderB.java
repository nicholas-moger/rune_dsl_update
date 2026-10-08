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
import holdout.voiddeeptok.meta.HolderBMeta;
import java.util.Objects;

import static java.util.Optional.ofNullable;

/**
 * The other option, carrying the same feature.
 * @version 0.0.0
 */
@RosettaDataType(value="HolderB", builder=HolderB.HolderBBuilderImpl.class, version="0.0.0")
@RuneDataType(value="HolderB", model="holdout", builder=HolderB.HolderBBuilderImpl.class, version="0.0.0")
public interface HolderB extends RosettaModelObject {

	HolderBMeta metaData = new HolderBMeta();

	/*********************** Getter Methods  ***********************/
	FieldWithMetaVoid getTok();

	/*********************** Build Methods  ***********************/
	HolderB build();
	
	HolderB.HolderBBuilder toBuilder();
	
	static HolderB.HolderBBuilder builder() {
		return new HolderB.HolderBBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends HolderB> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends HolderB> getType() {
		return HolderB.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processRosetta(path.newSubPath("tok"), processor, FieldWithMetaVoid.class, getTok());
	}
	

	/*********************** Builder Interface  ***********************/
	interface HolderBBuilder extends HolderB, RosettaModelObjectBuilder {
		FieldWithMetaVoid.FieldWithMetaVoidBuilder getOrCreateTok();
		@Override
		FieldWithMetaVoid.FieldWithMetaVoidBuilder getTok();
		HolderB.HolderBBuilder setTok(FieldWithMetaVoid tok);
		HolderB.HolderBBuilder setTokValue(Void tok);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processRosetta(path.newSubPath("tok"), processor, FieldWithMetaVoid.FieldWithMetaVoidBuilder.class, getTok());
		}
		

		HolderB.HolderBBuilder prune();
	}

	/*********************** Immutable Implementation of HolderB  ***********************/
	class HolderBImpl implements HolderB {
		private final FieldWithMetaVoid tok;
		
		protected HolderBImpl(HolderB.HolderBBuilder builder) {
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
		public HolderB build() {
			return this;
		}
		
		@Override
		public HolderB.HolderBBuilder toBuilder() {
			HolderB.HolderBBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(HolderB.HolderBBuilder builder) {
			ofNullable(getTok()).ifPresent(builder::setTok);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			HolderB _that = getType().cast(o);
		
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
			return "HolderB {" +
				"tok=" + this.tok +
			'}';
		}
	}

	/*********************** Builder Implementation of HolderB  ***********************/
	class HolderBBuilderImpl implements HolderB.HolderBBuilder {
	
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
		public HolderB.HolderBBuilder setTok(FieldWithMetaVoid _tok) {
			this.tok = _tok == null ? null : _tok.toBuilder();
			return this;
		}
		
		@Override
		public HolderB.HolderBBuilder setTokValue(Void _tok) {
			this.getOrCreateTok().setValue(_tok);
			return this;
		}
		
		@Override
		public HolderB build() {
			return new HolderB.HolderBImpl(this);
		}
		
		@Override
		public HolderB.HolderBBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public HolderB.HolderBBuilder prune() {
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
		public HolderB.HolderBBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			HolderB.HolderBBuilder o = (HolderB.HolderBBuilder) other;
			
			merger.mergeRosetta(getTok(), o.getTok(), this::setTok);
			
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			HolderB _that = getType().cast(o);
		
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
			return "HolderBBuilder {" +
				"tok=" + this.tok +
			'}';
		}
	}
}
