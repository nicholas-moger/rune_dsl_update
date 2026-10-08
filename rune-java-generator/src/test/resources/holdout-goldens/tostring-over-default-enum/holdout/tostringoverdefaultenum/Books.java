package holdout.tostringoverdefaultenum;

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
import com.rosetta.model.metafields.FieldWithMetaString;
import holdout.tostringoverdefaultenum.meta.BooksMeta;
import java.util.Objects;

import static java.util.Optional.ofNullable;

/**
 * The holder.
 * @version 0.0.0
 */
@RosettaDataType(value="Books", builder=Books.BooksBuilderImpl.class, version="0.0.0")
@RuneDataType(value="Books", model="holdout", builder=Books.BooksBuilderImpl.class, version="0.0.0")
public interface Books extends RosettaModelObject {

	BooksMeta metaData = new BooksMeta();

	/*********************** Getter Methods  ***********************/
	SideEnum getSide();
	FieldWithMetaString getCoded();

	/*********************** Build Methods  ***********************/
	Books build();
	
	Books.BooksBuilder toBuilder();
	
	static Books.BooksBuilder builder() {
		return new Books.BooksBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends Books> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends Books> getType() {
		return Books.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("side"), SideEnum.class, getSide(), this);
		processRosetta(path.newSubPath("coded"), processor, FieldWithMetaString.class, getCoded());
	}
	

	/*********************** Builder Interface  ***********************/
	interface BooksBuilder extends Books, RosettaModelObjectBuilder {
		FieldWithMetaString.FieldWithMetaStringBuilder getOrCreateCoded();
		@Override
		FieldWithMetaString.FieldWithMetaStringBuilder getCoded();
		Books.BooksBuilder setSide(SideEnum side);
		Books.BooksBuilder setCoded(FieldWithMetaString coded);
		Books.BooksBuilder setCodedValue(String coded);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("side"), SideEnum.class, getSide(), this);
			processRosetta(path.newSubPath("coded"), processor, FieldWithMetaString.FieldWithMetaStringBuilder.class, getCoded());
		}
		

		Books.BooksBuilder prune();
	}

	/*********************** Immutable Implementation of Books  ***********************/
	class BooksImpl implements Books {
		private final SideEnum side;
		private final FieldWithMetaString coded;
		
		protected BooksImpl(Books.BooksBuilder builder) {
			this.side = builder.getSide();
			this.coded = ofNullable(builder.getCoded()).map(f->f.build()).orElse(null);
		}
		
		@Override
		@RosettaAttribute("side")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("side")
		public SideEnum getSide() {
			return side;
		}
		
		@Override
		@RosettaAttribute("coded")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("coded")
		public FieldWithMetaString getCoded() {
			return coded;
		}
		
		@Override
		public Books build() {
			return this;
		}
		
		@Override
		public Books.BooksBuilder toBuilder() {
			Books.BooksBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(Books.BooksBuilder builder) {
			ofNullable(getSide()).ifPresent(builder::setSide);
			ofNullable(getCoded()).ifPresent(builder::setCoded);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Books _that = getType().cast(o);
		
			if (!Objects.equals(side, _that.getSide())) return false;
			if (!Objects.equals(coded, _that.getCoded())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (side != null ? side.getClass().getName().hashCode() : 0);
			_result = 31 * _result + (coded != null ? coded.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "Books {" +
				"side=" + this.side + ", " +
				"coded=" + this.coded +
			'}';
		}
	}

	/*********************** Builder Implementation of Books  ***********************/
	class BooksBuilderImpl implements Books.BooksBuilder {
	
		protected SideEnum side;
		protected FieldWithMetaString.FieldWithMetaStringBuilder coded;
		
		@Override
		@RosettaAttribute("side")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("side")
		public SideEnum getSide() {
			return side;
		}
		
		@Override
		@RosettaAttribute("coded")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("coded")
		public FieldWithMetaString.FieldWithMetaStringBuilder getCoded() {
			return coded;
		}
		
		@Override
		public FieldWithMetaString.FieldWithMetaStringBuilder getOrCreateCoded() {
			FieldWithMetaString.FieldWithMetaStringBuilder result;
			if (coded!=null) {
				result = coded;
			}
			else {
				result = coded = FieldWithMetaString.builder();
			}
			
			return result;
		}
		
		@RosettaAttribute("side")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("side")
		@Override
		public Books.BooksBuilder setSide(SideEnum _side) {
			this.side = _side == null ? null : _side;
			return this;
		}
		
		@RosettaAttribute("coded")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("coded")
		@Override
		public Books.BooksBuilder setCoded(FieldWithMetaString _coded) {
			this.coded = _coded == null ? null : _coded.toBuilder();
			return this;
		}
		
		@Override
		public Books.BooksBuilder setCodedValue(String _coded) {
			this.getOrCreateCoded().setValue(_coded);
			return this;
		}
		
		@Override
		public Books build() {
			return new Books.BooksImpl(this);
		}
		
		@Override
		public Books.BooksBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public Books.BooksBuilder prune() {
			if (coded!=null && !coded.prune().hasData()) coded = null;
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getSide()!=null) return true;
			if (getCoded()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public Books.BooksBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			Books.BooksBuilder o = (Books.BooksBuilder) other;
			
			merger.mergeRosetta(getCoded(), o.getCoded(), this::setCoded);
			
			merger.mergeBasic(getSide(), o.getSide(), this::setSide);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Books _that = getType().cast(o);
		
			if (!Objects.equals(side, _that.getSide())) return false;
			if (!Objects.equals(coded, _that.getCoded())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (side != null ? side.getClass().getName().hashCode() : 0);
			_result = 31 * _result + (coded != null ? coded.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "BooksBuilder {" +
				"side=" + this.side + ", " +
				"coded=" + this.coded +
			'}';
		}
	}
}
